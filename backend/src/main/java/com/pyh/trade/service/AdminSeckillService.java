package com.pyh.trade.service;

import com.pyh.trade.dto.ActivitySaveReq;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.IdVO;

import java.util.List;

/**
 * 运营端秒杀活动服务。
 * <p>创建活动与秒杀 SKU、预热库存到 Redis。预热是秒杀能开抢的前置动作：
 * 未预热时买家下单会走「自动回填」兜底，但会多一次 DB 查询。
 * 由 AdminSeckillController 调用。
 */
public interface AdminSeckillService {

    /**
     * 全部活动列表（含下线活动）。
     *
     * @return 活动列表
     */
    List<ActivityVO> list();

    /**
     * 创建活动及秒杀 SKU。
     *
     * @param req 入参
     * @return 活动 id
     */
    IdVO create(ActivitySaveReq req);

    /**
     * 预热：把 t_seckill_sku.stock 写入 Redis 秒杀库存 key。
     *
     * @param activityId 活动 id
     */
    void preheat(Long activityId);
}
