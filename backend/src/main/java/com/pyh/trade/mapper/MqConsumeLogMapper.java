package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.MqConsumeLog;

/**
 * MQ 消费幂等 Mapper。
 * <p>消费前 insert，唯一索引冲突（DuplicateKeyException）即判定重复投递。
 */
public interface MqConsumeLogMapper extends BaseMapper<MqConsumeLog> {
}
