package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消费幂等表实体（t_mq_consume_log）。
 * <p>消费者在处理消息前先插这条记录，{@code message_id} 唯一索引冲突说明重复投递，直接丢弃。
 * 只由 OrderCloseConsumer 写入。
 */
@Data
@TableName("t_mq_consume_log")
public class MqConsumeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一 id（生产者生成，消费端据此幂等） */
    private String messageId;

    private LocalDateTime consumeTime;
}
