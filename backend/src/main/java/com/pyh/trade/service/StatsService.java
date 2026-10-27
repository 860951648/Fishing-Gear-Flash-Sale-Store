package com.pyh.trade.service;

import com.pyh.trade.vo.SalesTrendVO;
import com.pyh.trade.vo.StatsOverviewVO;

import java.util.List;

/**
 * 运营看板统计服务。
 * <p>聚合 SQL 在 resources/mapper/StatsMapper.xml，本层负责口径（时间窗、金额精度、空日期补齐）。
 * 由 AdminStatsController 调用。
 */
public interface StatsService {

    /**
     * 看板概览：今日销售额、今日订单数、待审核退款数、库存预警数。
     *
     * @return 概览
     */
    StatsOverviewVO overview();

    /**
     * 最近 N 天销售趋势（按天补零）。
     *
     * @param days 天数，1-30
     * @return 趋势列表
     */
    List<SalesTrendVO> salesTrend(int days);
}
