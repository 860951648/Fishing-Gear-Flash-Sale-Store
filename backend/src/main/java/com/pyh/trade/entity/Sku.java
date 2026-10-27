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
 * SKU 实体（t_sku）。
 * <p>一个商品挂多个规格；秒杀活动按 SKU 维度配置秒杀价与秒杀库存，
 * 商品详情与秒杀下单都会读这张表。
 */
@Data
@TableName("t_sku")
public class Sku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 规格名，如「5.4 米 / 28 调」 */
    private String name;

    private BigDecimal price;

    /** 1 启用 / 0 停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
