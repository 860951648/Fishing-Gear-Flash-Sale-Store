package com.pyh.trade.controller;

import com.pyh.trade.common.Result;
import com.pyh.trade.dto.PayCallbackReq;
import com.pyh.trade.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口（接口契约 §3.3 第 13-14 个）。
 * <p>{@code /api/pay/mock/{orderNo}} 是本地模拟网关（需要登录），
 * {@code /api/pay/callback} 是网关回调（公开但必须验签）。两者最终调用同一个入账逻辑。
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PaymentService paymentService;

    public PayController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 本地模拟支付 */
    @PostMapping("/mock/{orderNo}")
    public Result<Void> mockPay(@PathVariable String orderNo) {
        paymentService.mockPay(orderNo);
        return Result.ok();
    }

    /** 第三方支付回调 */
    @PostMapping("/callback")
    public Result<Void> callback(@Valid @RequestBody PayCallbackReq req) {
        paymentService.callback(req);
        return Result.ok();
    }
}
