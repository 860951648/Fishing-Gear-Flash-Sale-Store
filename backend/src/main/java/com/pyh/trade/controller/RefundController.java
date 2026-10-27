package com.pyh.trade.controller;

import com.pyh.trade.common.PageResult;
import com.pyh.trade.common.Result;
import com.pyh.trade.dto.RefundApplyReq;
import com.pyh.trade.service.RefundService;
import com.pyh.trade.vo.RefundNoVO;
import com.pyh.trade.vo.RefundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家退款接口（接口契约 §3.3 第 15-16 个）。
 * <p>申请退款会把订单流转到 5 退款中，等待运营审核；业务在 RefundServiceImpl。
 */
@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 申请退款 */
    @PostMapping
    public Result<RefundNoVO> apply(@Valid @RequestBody RefundApplyReq req) {
        return Result.ok(refundService.apply(req));
    }

    /** 我的退款记录 */
    @GetMapping
    public Result<PageResult<RefundVO>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(refundService.pageMine(page, size));
    }
}
