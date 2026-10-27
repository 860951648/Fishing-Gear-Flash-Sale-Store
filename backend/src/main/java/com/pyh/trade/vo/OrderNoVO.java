package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀下单结果。
 * <p>只返回订单号，前端拿到后直接跳订单详情/支付页。
 */
@Data
public class OrderNoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    public OrderNoVO() {
    }

    public OrderNoVO(String orderNo) {
        this.orderNo = orderNo;
    }
}
