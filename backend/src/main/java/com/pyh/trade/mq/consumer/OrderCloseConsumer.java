package com.pyh.trade.mq.consumer;

import com.pyh.trade.config.RabbitMQConfig;
import com.pyh.trade.entity.MqConsumeLog;
import com.pyh.trade.mapper.MqConsumeLogMapper;
import com.pyh.trade.mq.message.OrderDelayMessage;
import com.pyh.trade.service.OrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 延迟关单消费者。
 * <p>只在 {@code app.mq.enabled=true} 时注册；本机没装 RabbitMQ 时这个 Bean 根本不会创建，
 * 也就不会启动监听容器。
 * <p>可靠性设计：
 * <ul>
 *   <li>手动 ACK：业务处理成功才 ack，抛异常 nack（不重回队列，避免毒消息无限循环）；</li>
 *   <li>幂等：先插 t_mq_consume_log（message_id 唯一），重复投递直接 ack 丢弃；</li>
 *   <li>关单本身也是幂等的：{@code closeOrder} 用状态机 CAS，重复调用返回 false 不报错。</li>
 * </ul>
 * 监听 {@code order.close.queue}（延迟队列的死信目标）。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderCloseConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseConsumer.class);

    private final OrderService orderService;
    private final MqConsumeLogMapper mqConsumeLogMapper;

    public OrderCloseConsumer(OrderService orderService, MqConsumeLogMapper mqConsumeLogMapper) {
        this.orderService = orderService;
        this.mqConsumeLogMapper = mqConsumeLogMapper;
    }

    /**
     * 消费延迟关单消息。
     *
     * @param message 消息体（JSON）
     * @param raw     AMQP 原始消息，用于取 messageId
     * @param channel 通道，用于手动 ack/nack
     * @throws IOException ack/nack 失败
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CLOSE_QUEUE)
    public void onMessage(OrderDelayMessage message, Message raw, Channel channel) throws IOException {
        long deliveryTag = raw.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageId() != null
                ? message.getMessageId()
                : raw.getMessageProperties().getMessageId();
        try {
            if (messageId == null || message.getOrderNo() == null) {
                log.warn("关单消息缺少必要字段，直接丢弃：{}", message);
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 幂等：同一 messageId 只处理一次
            MqConsumeLog consumeLog = new MqConsumeLog();
            consumeLog.setMessageId(messageId);
            consumeLog.setConsumeTime(LocalDateTime.now());
            mqConsumeLogMapper.insert(consumeLog);

            boolean closed = orderService.closeOrder(message.getOrderNo());
            log.info("关单消息处理完成 orderNo={} messageId={} closed={}",
                    message.getOrderNo(), messageId, closed);
            channel.basicAck(deliveryTag, false);
        } catch (DuplicateKeyException e) {
            // 重复投递：已消费过，ack 掉即可
            log.info("关单消息重复投递，跳过 messageId={}", messageId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("关单消息处理失败 messageId={} orderNo={}", messageId, message.getOrderNo(), e);
            // requeue=false：避免异常消息反复入队；关单最终由 OrderCloseTask 兜底
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
