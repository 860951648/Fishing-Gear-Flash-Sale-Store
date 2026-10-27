package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.StockLog;

/**
 * 库存流水 Mapper。
 * <p>写流水是关单/退款回滚库存前的幂等占位：(order_no, type) 唯一，重复动作直接抛 DuplicateKeyException。
 */
public interface StockLogMapper extends BaseMapper<StockLog> {
}
