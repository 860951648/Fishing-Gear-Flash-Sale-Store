package com.pyh.trade.controller;

import com.pyh.trade.common.Result;
import com.pyh.trade.service.StatsService;
import com.pyh.trade.vo.SalesTrendVO;
import com.pyh.trade.vo.StatsOverviewVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营看板接口（接口契约 §3.3 第 28-29 个）。
 * <p>要求 {@code stats:view} 权限；聚合 SQL 在 StatsMapper.xml，业务在 StatsServiceImpl。
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 看板概览 */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('stats:view')")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    /** 销售趋势（默认最近 7 天） */
    @GetMapping("/sales-trend")
    @PreAuthorize("hasAuthority('stats:view')")
    public Result<List<SalesTrendVO>> salesTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.salesTrend(days));
    }
}
