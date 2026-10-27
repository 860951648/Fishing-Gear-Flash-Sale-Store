package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.SeckillActivity;

/**
 * 秒杀活动 Mapper。
 * <p>买家端只列 status=1 且未结束的活动；运营端列全部。
 */
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {
}
