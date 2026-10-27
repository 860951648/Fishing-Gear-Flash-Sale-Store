package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.PaymentRecord;

/**
 * 支付流水 Mapper。
 * <p>支付回调按 channel_trade_no 查重（唯一索引兜底），成功后插入流水。
 */
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
