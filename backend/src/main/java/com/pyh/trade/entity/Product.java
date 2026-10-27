package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表实体（t_product）。
 * <p>由商品查询/运营端商品管理读写；{@code deleted} 为 MyBatis-Plus 逻辑删除标记，
 * 删除只置 1，历史订单仍能查到商品名（订单项里冗余了名称）。
 */
@Data
@TableName("t_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 副标题/卖点 */
    private String subtitle;

    /** 封面图 URL */
    private String cover;

    /** 展示价（SKU 最低价） */
    private BigDecimal price;

    /** 1 上架 / 0 下架 */
    private Integer status;

    /** 逻辑删除：0 未删 / 1 已删 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
