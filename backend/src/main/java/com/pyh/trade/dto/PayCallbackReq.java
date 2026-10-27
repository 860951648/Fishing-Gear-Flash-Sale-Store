package com.pyh.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调入参（第三方支付网关 → 本系统）。
 * <p>由 PayController#callback 接收，PaymentServiceImpl 先按
 * {@code HMAC_SHA256(orderNo|amount|timestamp|nonce, app.pay.secret)} 验签，
 * 再走「分布式锁 + 幂等表 + 状态机 CAS」的入账流程。
 * 这是全项目唯一允许出现“原始报文”语义的入参，仍用 DTO 而不是 Map。
 */
@Data
public class PayCallbackReq {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "渠道交易号不能为空")
    private String channelTradeNo;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于 0")
    private BigDecimal amount;

    /** 回调时间戳（秒），参与签名，用于防重放 */
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    /** 随机串，参与签名，保证同一笔回调的签名不可复用 */
    @NotBlank(message = "nonce 不能为空")
    private String nonce;

    /** HMAC-SHA256 签名（十六进制小写） */
    @NotBlank(message = "签名不能为空")
    private String sign;
}
