package com.pyh.trade.service;

import com.pyh.trade.vo.ActivityDetailVO;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.OrderNoVO;

import java.util.List;

/**
 * 秒杀服务（买家侧）。
 * <p>活动列表/详情（实时 Redis 库存）与秒杀下单。
 * 下单链路：Lua 令牌桶限流 → Lua 原子预扣（幂等标记 + 库存）→ 同步 DB 事务落单 → 失败补偿 Redis。
 * 由 SeckillController 调用。
 */
public interface SeckillService {

    /**
     * 进行中/未结束的活动列表。
     *
     * @return 活动列表
     */
    List<ActivityVO> listActivities();

    /**
     * 活动详情，含每个 SKU 的 Redis 实时库存。
     *
     * @param activityId 活动 id
     * @return 活动详情
     */
    ActivityDetailVO detail(Long activityId);

    /**
     * 秒杀下单。
     *
     * @param activityId 活动 id
     * @param skuId      秒杀 SKU id（t_seckill_sku 主键）
     * @return 订单号
     */
    OrderNoVO seckill(Long activityId, Long skuId);
}
