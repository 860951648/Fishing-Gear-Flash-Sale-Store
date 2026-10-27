package com.pyh.trade.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置（延迟关单）。
 * <p>本机没装 RabbitMQ，所以整份配置挂在 {@code app.mq.enabled=true} 下：
 * 关闭时既不声明队列也不启动监听容器，应用照样能起、下单走 @Scheduled 兜底关单。
 * <p>延迟实现方式：业务队列 {@code order.delay.queue} 不设消费者，只配 {@code x-message-ttl=1800000}（30 分钟）
 * 和死信转发；消息到期后由 Broker 投到 {@code order.dlx} → {@code order.close.queue}，消费者才真正处理。
 * 这样做的好处是不用装延迟插件，坏处是 TTL 队头阻塞（同一队列里长 TTL 消息会挡住短 TTL 消息），
 * 本项目只有一种 TTL，无影响。
 *
 * @see com.pyh.trade.mq.producer.OrderDelayProducer
 * @see com.pyh.trade.mq.consumer.OrderCloseConsumer
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitMQConfig {

    /** 延迟交换机：下单后把订单号发到这里 */
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    /** 延迟队列：没有消费者，只等 TTL 到期转死信 */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    /** 延迟队列路由键 */
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    /** 死信交换机 */
    public static final String ORDER_DLX_EXCHANGE = "order.dlx";
    /** 死信队列：真正的关单消费者监听它 */
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";
    /** 死信路由键 */
    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close";

    /**
     * 延迟交换机（direct，durable）。
     *
     * @return 交换机
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 延迟队列：TTL 30 分钟 + 死信配置。
     *
     * @return 队列
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>(4);
        // 消息在队列里最多待 30 分钟，对应订单支付超时时间
        args.put("x-message-ttl", 1800000);
        // 到期后不丢弃，转发到死信交换机，由关单队列消费
        args.put("x-dead-letter-exchange", ORDER_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CLOSE_ROUTING_KEY);
        return QueueBuilder.durable(ORDER_DELAY_QUEUE).withArguments(args).build();
    }

    /**
     * 延迟交换机与延迟队列绑定。
     *
     * @return 绑定关系
     */
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 死信交换机。
     *
     * @return 交换机
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE, true, false);
    }

    /**
     * 关单队列（真正被消费的队列）。
     *
     * @return 队列
     */
    @Bean
    public Queue orderCloseQueue() {
        return QueueBuilder.durable(ORDER_CLOSE_QUEUE).build();
    }

    /**
     * 死信交换机与关单队列绑定。
     *
     * @return 绑定关系
     */
    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue()).to(orderDlxExchange()).with(ORDER_CLOSE_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器：消息体可读，且消费端能直接反序列化成 OrderDelayMessage。
     *
     * @return 转换器
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
