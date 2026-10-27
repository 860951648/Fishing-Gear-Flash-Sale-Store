package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情出参。
 * <p>订单主信息 + 明细列表，由 OrderServiceImpl#detail 组装；只能查自己的订单（运营端走订单列表）。
 */
@Data
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    private Long activityId;

    private Long seckillSkuId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    /** 0 待支付 1 已支付 2 已发货 3 已完成 4 已关闭 5 退款中 6 已退款 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}
