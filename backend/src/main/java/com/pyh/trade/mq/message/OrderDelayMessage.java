package com.pyh.trade.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单延迟关单消息。
 * <p>下单成功后投到 {@code order.delay.exchange}，30 分钟 TTL 到期后由死信队列投递给消费者。
 * 消息里带 messageId 用于消费幂等（t_mq_consume_log.message_id 唯一）。
 * 由 OrderDelayProducer 发送、OrderCloseConsumer 接收。
 */
@Data
public class OrderDelayMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号，关单的唯一依据 */
    private String orderNo;

    /** 消息唯一 id，消费端写 t_mq_consume_log 做幂等 */
    private String messageId;

    /** 发送时间，便于排查延迟 */
    private LocalDateTime sendTime;
}
