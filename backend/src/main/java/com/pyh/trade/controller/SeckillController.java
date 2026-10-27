package com.pyh.trade.controller;

import com.pyh.trade.common.Result;
import com.pyh.trade.service.SeckillService;
import com.pyh.trade.vo.ActivityDetailVO;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.OrderNoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀接口（接口契约 §3.3 第 7-9 个）。
 * <p>活动列表/详情公开；下单需要登录，业务在 SeckillServiceImpl（限流 → Lua 预扣 → 同步落库）。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /** 进行中的活动列表 */
    @GetMapping("/activities")
    public Result<List<ActivityVO>> activities() {
        return Result.ok(seckillService.listActivities());
    }

    /** 活动详情（含 Redis 实时库存） */
    @GetMapping("/activities/{id}")
    public Result<ActivityDetailVO> activityDetail(@PathVariable Long id) {
        return Result.ok(seckillService.detail(id));
    }

    /** 秒杀下单：路径里的 skuId 是秒杀 SKU 主键（t_seckill_sku.id） */
    @PostMapping("/{activityId}/skus/{skuId}")
    public Result<OrderNoVO> seckill(@PathVariable Long activityId, @PathVariable Long skuId) {
        return Result.ok(seckillService.seckill(activityId, skuId));
    }
}
