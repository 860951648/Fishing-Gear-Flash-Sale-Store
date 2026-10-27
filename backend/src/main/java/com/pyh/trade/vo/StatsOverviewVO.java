package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 运营看板概览出参。
 * <p>todaySales 今日支付金额（仅统计已支付及之后的状态）、orderCount 今日订单数、
 * pendingRefund 待审核退款数、stockWarn 可用库存低于阈值的 SKU 数。
 * 由 StatsServiceImpl 调用 StatsMapper（XML 聚合 SQL）得到。
 */
@Data
public class StatsOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 今日销售额 */
    private BigDecimal todaySales;

    /** 今日订单数 */
    private Long orderCount;

    /** 待审核退款单数 */
    private Long pendingRefund;

    /** 库存预警 SKU 数 */
    private Long stockWarn;
}
