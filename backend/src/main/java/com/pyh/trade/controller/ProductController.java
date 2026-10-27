package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.service.ProductService;
import com.pyh.trade.vo.ProductDetailVO;
import com.pyh.trade.vo.ProductVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品浏览接口（接口契约 §3.3 第 5-6 个）。
 * <p>公开接口，不需要登录；详情走 Redis 逻辑过期缓存。业务在 ProductServiceImpl。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 上架商品分页 */
    @GetMapping
    public Result<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(productService.page(page, size, keyword));
    }

    /** 商品详情（含 SKU 与库存） */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }
}
