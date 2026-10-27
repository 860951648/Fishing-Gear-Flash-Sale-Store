package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体（t_order）。
 * <p>状态机 0 待支付 → 1 已支付 → 2 已发货 → 3 已完成 / 0 → 4 已关闭 / 1,2,3 → 5 退款中 → 6 已退款。
 * 所有状态流转都走 {@code WHERE status = #{from}} 的条件 UPDATE（见 OrderMapper），影响 0 行即 409。
 * 唯一索引 {@code uk_user_seckill(user_id, seckill_sku_id)} 兜住秒杀重复下单。
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号（对外唯一标识，用于支付/退款/关单） */
    private String orderNo;

    private Long userId;

    /** 秒杀活动 id，普通订单为空 */
    private Long activityId;

    /** 秒杀 SKU id，普通订单为空；与 user_id 组成唯一索引防重复抢购 */
    private Long seckillSkuId;

    /** 订单原价 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 0 待支付 1 已支付 2 已发货 3 已完成 4 已关闭 5 退款中 6 已退款 */
    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime closeTime;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
