package com.pyh.trade.mapper;

import com.pyh.trade.vo.SalesTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营看板统计 Mapper。
 * <p>聚合 SQL 写在 resources/mapper/StatsMapper.xml 里（GROUP BY / SUM 用注解会很难读），
 * 由 StatsServiceImpl 调用；参数全部 #{} 预编译。
 */
@Mapper
public interface StatsMapper {

    /**
     * 统计时间窗内的支付金额。
     *
     * @param start 起始时间（含）
     * @param end   结束时间（不含）
     * @return 金额合计，无数据返回 0
     */
    BigDecimal selectSalesAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计时间窗内创建的订单数。
     *
     * @param start 起始时间（含）
     * @param end   结束时间（不含）
     * @return 订单数
     */
    Long selectOrderCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 待审核退款单数量。
     *
     * @return 数量
     */
    Long selectPendingRefundCount();

    /**
     * 可用库存低于阈值的 SKU 数量。
     *
     * @param threshold 阈值
     * @return 数量
     */
    Long selectStockWarnCount(@Param("threshold") Integer threshold);

    /**
     * 按天统计销售额（最近 N 天，含今天）。
     *
     * @param start 起始时间（含）
     * @return 日期 + 金额列表
     */
    List<SalesTrendVO> selectSalesTrend(@Param("start") LocalDateTime start);
}
