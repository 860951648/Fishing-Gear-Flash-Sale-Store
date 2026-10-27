package com.pyh.trade.service;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.dto.ProductSaveReq;
import com.pyh.trade.dto.StockUpdateReq;
import com.pyh.trade.vo.IdVO;
import com.pyh.trade.vo.ProductVO;

/**
 * 运营端商品服务。
 * <p>商品/ SKU / 库存的增删改，全部在一个事务里完成；
 * 每次写操作后删除商品详情缓存，避免前端看到旧价格旧库存。
 * 由 AdminProductController 调用。
 */
public interface AdminProductService {

    /**
     * 运营端商品分页（含下架商品）。
     *
     * @param page    页码
     * @param size    每页条数
     * @param keyword 关键字，可空
     * @return 分页结果
     */
    PageResult<ProductVO> page(int page, int size, String keyword);

    /**
     * 新增商品（含 SKU 与初始库存）。
     *
     * @param req 入参
     * @return 新商品 id
     */
    IdVO create(ProductSaveReq req);

    /**
     * 编辑商品（SKU 有 id 则更新，无 id 则新增）。
     *
     * @param id  商品 id
     * @param req 入参
     */
    void update(Long id, ProductSaveReq req);

    /**
     * 逻辑删除商品（下架并置 deleted=1，不删历史 SKU 数据）。
     *
     * @param id 商品 id
     */
    void delete(Long id);

    /**
     * 直接调整 SKU 可用库存（运营补货）。
     *
     * @param skuId SKU id
     * @param req   入参
     */
    void updateStock(Long skuId, StockUpdateReq req);
}
