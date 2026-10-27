package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.dto.RefundAuditReq;
import com.pyh.trade.service.RefundService;
import com.pyh.trade.vo.RefundVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营端退款审核接口（接口契约 §3.3 第 26-27 个）。
 * <p>要求 {@code refund:audit} 权限；业务在 RefundServiceImpl（审核通过会回滚库存）。
 */
@RestController
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {

    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 退款单分页（可按状态筛选） */
    @GetMapping
    @PreAuthorize("hasAuthority('refund:audit')")
    public Result<PageResult<RefundVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(refundService.pageAdmin(page, size, status));
    }

    /** 审核退款 */
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('refund:audit')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody RefundAuditReq req) {
        refundService.audit(id, req);
        return Result.ok();
    }
}
