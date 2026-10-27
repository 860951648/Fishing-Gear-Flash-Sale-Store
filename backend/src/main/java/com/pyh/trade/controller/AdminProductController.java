package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.dto.ProductSaveReq;
import com.pyh.trade.dto.StockUpdateReq;
import com.pyh.trade.service.AdminProductService;
import com.pyh.trade.vo.IdVO;
import com.pyh.trade.vo.ProductVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营端商品管理接口（接口契约 §3.3 第 17-21 个）。
 * <p>全部要求 {@code product:manage} 权限；只做参数校验与转发，业务在 AdminProductServiceImpl。
 * 权限码在方法上声明（@PreAuthorize），JWT 过滤器只负责把权限码放进 Authentication。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    /** 运营端商品分页（含下架） */
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(adminProductService.page(page, size, keyword));
    }

    /** 新增商品 */
    @PostMapping("/products")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<IdVO> create(@Valid @RequestBody ProductSaveReq req) {
        return Result.ok(adminProductService.create(req));
    }

    /** 编辑商品 */
    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveReq req) {
        adminProductService.update(id, req);
        return Result.ok();
    }

    /** 逻辑删除商品 */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return Result.ok();
    }

    /** 调整 SKU 可用库存 */
    @PutMapping("/skus/{id}/stock")
    @PreAuthorize("hasAuthority('product:manage')")
    public Result<Void> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateReq req) {
        adminProductService.updateStock(id, req);
        return Result.ok();
    }
}
