package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项出参。
 * <p>商品名/SKU 名/数量取订单明细的第一条（A 项目每单一个秒杀商品），
 * 避免列表页为每行再查一次明细。
 */
@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    /** 下单用户 id，运营端列表用来定位买家 */
    private Long userId;

    private Long activityId;

    private Long seckillSkuId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    /** 0 待支付 1 已支付 2 已发货 3 已完成 4 已关闭 5 退款中 6 已退款 */
    private Integer status;

    /** 商品名快照 */
    private String productName;

    /** SKU 名快照 */
    private String skuName;

    private Integer quantity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
