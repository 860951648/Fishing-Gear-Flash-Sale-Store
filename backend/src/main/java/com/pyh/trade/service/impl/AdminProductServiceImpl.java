package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.dto.ProductSaveReq;
import com.pyh.trade.dto.StockUpdateReq;
import com.pyh.trade.entity.Product;
import com.pyh.trade.entity.Sku;
import com.pyh.trade.entity.Stock;
import com.pyh.trade.mapper.ProductMapper;
import com.pyh.trade.mapper.SkuMapper;
import com.pyh.trade.mapper.StockMapper;
import com.pyh.trade.service.AdminProductService;
import com.pyh.trade.service.ProductService;
import com.pyh.trade.vo.IdVO;
import com.pyh.trade.vo.ProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营端商品服务实现。
 * <p>所有写操作在一个事务里完成（商品 + SKU + 库存三者要么全成要么全不成），
 * 提交后删除商品详情缓存，避免前端继续看到旧价格/旧库存。
 * 被 AdminProductController 调用。
 */
@Service
public class AdminProductServiceImpl implements AdminProductService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final StockMapper stockMapper;
    private final ProductService productService;

    public AdminProductServiceImpl(ProductMapper productMapper, SkuMapper skuMapper,
                                   StockMapper stockMapper, ProductService productService) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.stockMapper = stockMapper;
        this.productService = productService;
    }

    @Override
    public PageResult<ProductVO> page(int page, int size, String keyword) {
        Page<Product> pageParam = new Page<>(Math.max(page, 1), normalizeSize(size));
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().orderByDesc(Product::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getSubtitle, keyword));
        }
        return PageResult.of(productMapper.selectPage(pageParam, wrapper), this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdVO create(ProductSaveReq req) {
        Product product = new Product();
        product.setName(req.getName());
        product.setSubtitle(req.getSubtitle());
        product.setCover(req.getCover());
        product.setPrice(scale(req.getPrice()));
        product.setStatus(Constants.ENABLED);
        product.setDeleted(0);
        productMapper.insert(product);

        for (ProductSaveReq.SkuSaveReq skuReq : req.getSkus()) {
            Sku sku = new Sku();
            sku.setProductId(product.getId());
            sku.setName(skuReq.getName());
            sku.setPrice(scale(skuReq.getPrice()));
            sku.setStatus(Constants.ENABLED);
            skuMapper.insert(sku);

            int stockValue = skuReq.getStock() == null ? 0 : skuReq.getStock();
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setTotal(stockValue);
            stock.setAvailable(stockValue);
            stock.setLocked(0);
            stock.setVersion(0);
            stockMapper.insert(stock);
        }
        log.info("运营新增商品 productId={} skuCount={}", product.getId(), req.getSkus().size());
        return new IdVO(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveReq req) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        product.setName(req.getName());
        product.setSubtitle(req.getSubtitle());
        product.setCover(req.getCover());
        product.setPrice(scale(req.getPrice()));
        productMapper.updateById(product);

        for (ProductSaveReq.SkuSaveReq skuReq : req.getSkus()) {
            if (skuReq.getId() != null) {
                // 已有 SKU：只改名称和价格，库存走独立的改库存接口，避免误覆盖被秒杀扣过的库存
                Sku sku = skuMapper.selectById(skuReq.getId());
                if (sku == null || !sku.getProductId().equals(id)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "SKU 不属于该商品：" + skuReq.getId());
                }
                sku.setName(skuReq.getName());
                sku.setPrice(scale(skuReq.getPrice()));
                skuMapper.updateById(sku);
            } else {
                Sku sku = new Sku();
                sku.setProductId(id);
                sku.setName(skuReq.getName());
                sku.setPrice(scale(skuReq.getPrice()));
                sku.setStatus(Constants.ENABLED);
                skuMapper.insert(sku);

                int stockValue = skuReq.getStock() == null ? 0 : skuReq.getStock();
                Stock stock = new Stock();
                stock.setSkuId(sku.getId());
                stock.setTotal(stockValue);
                stock.setAvailable(stockValue);
                stock.setLocked(0);
                stock.setVersion(0);
                stockMapper.insert(stock);
            }
        }
        productService.evictDetailCache(id);
        log.info("运营编辑商品 productId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        // @TableLogic：这里只置 deleted=1，历史订单的商品名快照仍在
        productMapper.deleteById(id);
        productService.evictDetailCache(id);
        log.info("运营删除商品（逻辑删除）productId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long skuId, StockUpdateReq req) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在");
        }
        if (stockMapper.updateAvailable(skuId, req.getAvailable()) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "库存记录不存在，请先初始化");
        }
        productService.evictDetailCache(sku.getProductId());
        log.info("运营调整库存 skuId={} available={}", skuId, req.getAvailable());
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setCover(product.getCover());
        vo.setPrice(product.getPrice());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private long normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, Constants.MAX_PAGE_SIZE);
    }
}
