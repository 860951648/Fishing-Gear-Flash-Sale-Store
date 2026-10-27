package com.pyh.trade.service;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.vo.ProductDetailVO;
import com.pyh.trade.vo.ProductVO;

/**
 * 商品查询服务（买家/公开接口）。
 * <p>列表分页查库；详情走 Redis 逻辑过期缓存（空值缓存防穿透）。
 * 由 ProductController 调用；运营端改动商品后由 {@link AdminProductService} 调用本接口的
 * {@link #evictDetailCache(Long)} 删缓存。
 */
public interface ProductService {

    /**
     * 上架商品分页。
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 名称/副标题模糊关键字，可空
     * @return 分页结果
     */
    PageResult<ProductVO> page(int page, int size, String keyword);

    /**
     * 商品详情（含 SKU 与库存）。
     *
     * @param id 商品 id
     * @return 详情
     */
    ProductDetailVO detail(Long id);

    /**
     * 删除商品详情缓存（运营端改价/改库存/删除后调用）。
     *
     * @param productId 商品 id
     */
    void evictDetailCache(Long productId);
}
