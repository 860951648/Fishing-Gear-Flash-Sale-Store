package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体（t_payment_record）。
 * <p>支付回调成功后落一条；{@code channel_trade_no} 唯一索引是重复回调的第二道幂等闸门
 * （第一道是 Redis 分布式锁 + 订单状态 CAS）。
 */
@Data
@TableName("t_payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    /** 渠道交易号，唯一 */
    private String channelTradeNo;

    private BigDecimal amount;

    /** 1 成功 / 0 失败 */
    private Integer status;

    /** 回调原始报文（排查对账用） */
    private String rawBody;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
