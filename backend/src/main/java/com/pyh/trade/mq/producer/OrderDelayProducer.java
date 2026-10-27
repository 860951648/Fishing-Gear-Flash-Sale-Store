package com.pyh.trade.mq.producer;

import com.pyh.trade.config.RabbitMQConfig;
import com.pyh.trade.mq.message.OrderDelayMessage;
import com.pyh.trade.utils.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 延迟关单消息生产者。
 * <p>发送动作在「订单事务提交之后」执行（调用点在 SeckillServiceImpl），
 * 避免消费者拿到消息时订单还没提交、查不到订单而直接跳过。
 * <p>MQ 未启用或发送失败时不抛异常：本机没装 RabbitMQ 也能下单，
 * 关单由 OrderCloseTask（每分钟扫表）兜底，用户不会因此卡住。
 * 被 SeckillServiceImpl 调用。
 */
@Component
public class OrderDelayProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderDelayProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mq.enabled:false}")
    private boolean mqEnabled;

    public OrderDelayProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送延迟关单消息（30 分钟后进入关单队列）。
     *
     * @param orderNo 订单号
     */
    public void sendCloseDelayMessage(String orderNo) {
        if (!mqEnabled) {
            // 关闭 MQ 时不连 Broker，日志里留痕便于演示说明降级路径
            log.info("MQ 未启用，订单 {} 的关单交由 OrderCloseTask 兜底", orderNo);
            return;
        }
        OrderDelayMessage message = new OrderDelayMessage();
        message.setOrderNo(orderNo);
        message.setMessageId(IdGenerator.uuid());
        message.setSendTime(LocalDateTime.now());
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY, message, m -> {
                        // 把 messageId 同时写进 AMQP 消息属性，方便在管控台按 id 查
                        m.getMessageProperties().setMessageId(message.getMessageId());
                        return m;
                    });
            log.info("延迟关单消息已发送 orderNo={} messageId={}", orderNo, message.getMessageId());
        } catch (AmqpException e) {
            // MQ 抖动不影响下单主流程：兜底任务每分钟扫一次超时订单
            log.error("延迟关单消息发送失败，降级为定时任务关单 orderNo={}", orderNo, e);
        }
    }
}
