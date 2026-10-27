package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细出参。
 * <p>订单详情页展示每一行的商品快照（下单时的名称与价格）。
 */
@Data
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private String productName;

    private String skuName;

    private BigDecimal price;

    private Integer quantity;
}
