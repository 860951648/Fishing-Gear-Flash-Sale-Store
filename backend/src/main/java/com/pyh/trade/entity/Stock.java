package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SKU 库存实体（t_stock）。
 * <p>可用库存 available 与锁定库存 locked 分离：下单扣 available 加 locked，
 * 关单/退款再还回去；{@code version} 支持 MyBatis-Plus 乐观锁。
 * 秒杀扣减用的是条件 UPDATE（available >= n），不依赖乐观锁重试。
 */
@Data
@TableName("t_stock")
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    /** 总库存 */
    private Integer total;

    /** 可用库存 */
    private Integer available;

    /** 锁定库存（已下单未结束） */
    private Integer locked;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
