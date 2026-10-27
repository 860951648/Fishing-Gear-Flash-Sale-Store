package com.pyh.trade.service;

import com.pyh.trade.dto.PayCallbackReq;

/**
 * 支付服务。
 * <p>本地模拟网关（mockPay）与真实回调（callback）走同一套入账逻辑：
 * 验签 → 分布式锁 → 幂等表查重 → 订单状态 CAS → 落支付流水 → Lua 释放锁。
 * 由 PayController 调用。
 */
public interface PaymentService {

    /**
     * 本地模拟支付：生成签名后走回调入账逻辑，方便没有真实网关时演示。
     *
     * @param orderNo 订单号
     */
    void mockPay(String orderNo);

    /**
     * 支付回调入账（验签 + 幂等 + 状态机 CAS）。
     *
     * @param req 回调报文
     */
    void callback(PayCallbackReq req);
}
