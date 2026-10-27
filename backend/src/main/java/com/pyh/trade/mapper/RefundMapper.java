package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.Refund;

/**
 * 退款单 Mapper。
 * <p>买家端按 userId 分页、运营端全量分页、审核时更新 status/audit_remark。
 */
public interface RefundMapper extends BaseMapper<Refund> {
}
