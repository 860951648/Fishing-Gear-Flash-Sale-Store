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
 * 秒杀 SKU 实体（t_seckill_sku）。
 * <p>活动与 SKU 的唯一组合，存秒杀价与秒杀库存；
 * 下单时先扣 Redis 预扣库存，再以 {@code stock >= 1} 条件 UPDATE 落库，两个库存最终一致。
 */
@Data
@TableName("t_seckill_sku")
public class SeckillSku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long skuId;

    private BigDecimal seckillPrice;

    /** 秒杀剩余库存（DB 侧，Redis 预扣成功后同步扣减） */
    private Integer stock;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
