package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.OrderItem;

/**
 * 订单明细 Mapper。
 * <p>下单时批量插入、订单详情按 orderId 查列表。
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
