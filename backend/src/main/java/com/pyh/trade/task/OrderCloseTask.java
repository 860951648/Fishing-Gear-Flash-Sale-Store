package com.pyh.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyh.trade.common.Constants;
import com.pyh.trade.entity.Order;
import com.pyh.trade.mapper.OrderMapper;
import com.pyh.trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时订单关单兜底任务。
 * <p>为什么需要它：RabbitMQ 没装（{@code app.mq.enabled=false}）或 MQ 抖动的场景下，
 * 延迟消息不生效，必须有「扫表关单」的第二条路径，否则订单会永远停在待支付、库存永远被锁定。
 * <p>两条路径最终都调用同一个幂等 {@code OrderService#closeOrder}，谁先抢到状态机 CAS 谁生效。
 * 每分钟扫一次，批量 100 条，避免一次锁太多行。
 */
@Component
public class OrderCloseTask {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseTask.class);

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public OrderCloseTask(OrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    /**
     * 每分钟扫描：status=0 且创建时间早于「现在 - 30 分钟」的订单，逐单幂等关单。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(Constants.ORDER_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, Constants.ORDER_STATUS_UNPAID)
                .lt(Order::getCreateTime, deadline)
                .orderByAsc(Order::getId)
                .last("LIMIT " + Constants.CLOSE_ORDER_BATCH));
        if (timeoutOrders.isEmpty()) {
            return;
        }
        int closed = 0;
        for (Order order : timeoutOrders) {
            if (orderService.closeOrder(order.getOrderNo())) {
                closed++;
            }
        }
        log.info("超时关单任务执行完成：扫描 {} 单，关闭 {} 单", timeoutOrders.size(), closed);
    }
}
