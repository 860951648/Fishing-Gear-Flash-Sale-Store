package com.pyh.trade.service.impl;

import com.pyh.trade.common.Constants;
import com.pyh.trade.mapper.StatsMapper;
import com.pyh.trade.service.StatsService;
import com.pyh.trade.vo.SalesTrendVO;
import com.pyh.trade.vo.StatsOverviewVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营看板统计服务实现。
 * <p>聚合交给 StatsMapper.xml（GROUP BY / SUM 在 SQL 里做，避免把全表拉回内存）；
 * 本层只负责口径与展示细节：金额统一两位小数 HALF_UP、趋势按天补零。
 * 被 AdminStatsController 调用。
 */
@Service
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatsMapper statsMapper;

    public StatsServiceImpl(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    @Override
    public StatsOverviewVO overview() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        BigDecimal todaySales = statsMapper.selectSalesAmount(startOfDay, startOfNextDay);
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTodaySales(todaySales == null ? BigDecimal.ZERO.setScale(2) : todaySales.setScale(2, RoundingMode.HALF_UP));
        Long orderCount = statsMapper.selectOrderCount(startOfDay, startOfNextDay);
        vo.setOrderCount(orderCount == null ? 0L : orderCount);
        Long pendingRefund = statsMapper.selectPendingRefundCount();
        vo.setPendingRefund(pendingRefund == null ? 0L : pendingRefund);
        Long stockWarn = statsMapper.selectStockWarnCount(Constants.STOCK_WARN_THRESHOLD);
        vo.setStockWarn(stockWarn == null ? 0L : stockWarn);
        return vo;
    }

    @Override
    public List<SalesTrendVO> salesTrend(int days) {
        int window = Math.min(Math.max(days, 1), 30);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(window - 1L).atStartOfDay();

        List<SalesTrendVO> queried = statsMapper.selectSalesTrend(start);
        Map<String, BigDecimal> amountMap = new HashMap<>();
        if (queried != null) {
            for (SalesTrendVO item : queried) {
                if (item.getDate() != null) {
                    amountMap.put(item.getDate(), item.getAmount() == null
                            ? BigDecimal.ZERO : item.getAmount());
                }
            }
        }
        // 补齐没有订单的日期：前端折线图需要连续的 X 轴，否则今天没成交就会缺一个点
        List<SalesTrendVO> result = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            String date = today.minusDays(window - 1L - i).format(DATE_FORMATTER);
            BigDecimal amount = amountMap.getOrDefault(date, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(new SalesTrendVO(date, amount));
        }
        return result;
    }
}
