package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.entity.Product;
import com.pyh.trade.entity.Sku;
import com.pyh.trade.entity.Stock;
import com.pyh.trade.mapper.ProductMapper;
import com.pyh.trade.mapper.SkuMapper;
import com.pyh.trade.mapper.StockMapper;
import com.pyh.trade.service.ProductService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.vo.ProductDetailVO;
import com.pyh.trade.vo.ProductVO;
import com.pyh.trade.vo.SkuVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品查询服务实现。
 * <p>详情缓存方案：Redis Hash {@code trade:product:{id}}，字段 {@code data}（JSON）+ {@code expireAt}（毫秒时间戳）。
 * <p>为什么用「逻辑过期」而不是给 key 设 TTL：秒杀/大促时热点商品同时过期，大量请求会一起回源打穿数据库；
 * 逻辑过期让缓存在物理上一直存在，值过期后由「抢到重建锁」的那个线程刷新，其他请求先拿旧值返回。
 * 空值缓存 60 秒用于防穿透（查询不存在的商品不会每次打到 DB）。
 * 由 ProductController 与 AdminProductServiceImpl（改完商品删缓存）调用。
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> unlockScript;

    public ProductServiceImpl(ProductMapper productMapper, SkuMapper skuMapper, StockMapper stockMapper,
                              StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
                              @Qualifier("unlockScript") DefaultRedisScript<Long> unlockScript) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.stockMapper = stockMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.unlockScript = unlockScript;
    }

    @Override
    public PageResult<ProductVO> page(int page, int size, String keyword) {
        Page<Product> pageParam = new Page<>(normalizePage(page), normalizeSize(size));
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, Constants.ENABLED)
                .orderByDesc(Product::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getSubtitle, keyword));
        }
        return PageResult.of(productMapper.selectPage(pageParam, wrapper), this::toVO);
    }

    @Override
    public ProductDetailVO detail(Long id) {
        String cacheKey = RedisKeys.productDetail(id);
        long now = System.currentTimeMillis();
        Map<Object, Object> cache = stringRedisTemplate.opsForHash().entries(cacheKey);
        if (!cache.isEmpty()) {
            String data = cache.get("data") == null ? null : String.valueOf(cache.get("data"));
            long expireAt = parseLong(cache.get("expireAt"), 0L);
            if (expireAt > now) {
                if (!StringUtils.hasText(data)) {
                    // 空值缓存命中：说明这个商品不存在或已下架
                    throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
                }
                return readJson(data);
            }
            // 逻辑过期：抢到锁的线程同步重建，抢不到的先用旧值兜底（不阻塞用户）
            String lockKey = RedisKeys.productLock(id);
            String token = IdGenerator.uuid();
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(locked)) {
                try {
                    ProductDetailVO fresh = loadFromDb(id);
                    writeCache(id, fresh);
                    return requireFound(fresh);
                } finally {
                    // 比对持有者再删锁，避免误删别人的锁
                    stringRedisTemplate.execute(unlockScript, Collections.singletonList(lockKey), token);
                }
            }
            if (StringUtils.hasText(data)) {
                log.debug("商品缓存逻辑过期且未抢到重建锁，返回旧值 productId={}", id);
                return readJson(data);
            }
            // 旧值不可用（空值缓存刚过期）又不持有锁：直接回源，保证用户能看到上架后的商品
            ProductDetailVO fallback = loadFromDb(id);
            writeCache(id, fallback);
            return requireFound(fallback);
        }

        // 完全未命中：回源并写缓存（空值也写，60 秒后自然过期）
        ProductDetailVO vo = loadFromDb(id);
        writeCache(id, vo);
        return requireFound(vo);
    }

    @Override
    public void evictDetailCache(Long productId) {
        stringRedisTemplate.delete(RedisKeys.productDetail(productId));
        log.info("商品详情缓存已删除 productId={}", productId);
    }

    /**
     * 回源查询商品详情；商品不存在或已下架返回 null（由调用方决定缓存空值还是抛 404）。
     */
    private ProductDetailVO loadFromDb(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != Constants.ENABLED) {
            return null;
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, id)
                .eq(Sku::getStatus, Constants.ENABLED)
                .orderByAsc(Sku::getId));
        Map<Long, Stock> stockMap = new HashMap<>();
        if (!skus.isEmpty()) {
            List<Long> skuIds = skus.stream().map(Sku::getId).collect(Collectors.toList());
            stockMap = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getSkuId, skuIds))
                    .stream().collect(Collectors.toMap(Stock::getSkuId, s -> s, (a, b) -> a));
        }
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setCover(product.getCover());
        vo.setPrice(product.getPrice());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        List<SkuVO> skuVOs = new ArrayList<>(skus.size());
        for (Sku sku : skus) {
            SkuVO skuVO = new SkuVO();
            skuVO.setId(sku.getId());
            skuVO.setName(sku.getName());
            skuVO.setPrice(sku.getPrice());
            Stock stock = stockMap.get(sku.getId());
            skuVO.setAvailable(stock == null || stock.getAvailable() == null ? 0 : stock.getAvailable());
            skuVO.setLocked(stock == null || stock.getLocked() == null ? 0 : stock.getLocked());
            skuVOs.add(skuVO);
        }
        vo.setSkus(skuVOs);
        return vo;
    }

    /**
     * 写缓存：null 走空值缓存（60 秒），正常值走逻辑过期（5 分钟）。
     * 物理 TTL 比逻辑过期多 1 分钟，避免 key 被 Redis 提前回收导致逻辑过期失效。
     */
    private void writeCache(Long id, ProductDetailVO vo) {
        Map<String, String> hash = new HashMap<>(4);
        if (vo == null) {
            hash.put("data", "");
            hash.put("expireAt", String.valueOf(System.currentTimeMillis() + Constants.NULL_CACHE_SECONDS * 1000L));
        } else {
            try {
                hash.put("data", objectMapper.writeValueAsString(vo));
            } catch (JacksonException e) {
                log.error("商品详情序列化失败 productId={}", id, e);
                return;
            }
            hash.put("expireAt", String.valueOf(
                    System.currentTimeMillis() + Constants.PRODUCT_CACHE_MINUTES * 60_000L));
        }
        String cacheKey = RedisKeys.productDetail(id);
        stringRedisTemplate.opsForHash().putAll(cacheKey, hash);
        stringRedisTemplate.expire(cacheKey, Duration.ofMinutes(Constants.PRODUCT_CACHE_MINUTES + 1L));
    }

    private ProductDetailVO requireFound(ProductDetailVO vo) {
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return vo;
    }

    private ProductDetailVO readJson(String data) {
        try {
            return objectMapper.readValue(data, ProductDetailVO.class);
        } catch (JacksonException e) {
            // 缓存内容异常（例如结构变更）时不让用户看到 500，回源重建
            log.warn("商品缓存反序列化失败，回源重建：{}", e.getMessage());
            throw new BusinessException(ResultCode.SERVER_ERROR, "缓存数据异常，请重试");
        }
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

    private long normalizePage(int page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, Constants.MAX_PAGE_SIZE);
    }
}
