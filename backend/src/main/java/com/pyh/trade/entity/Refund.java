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
 * 退款单实体（t_refund）。
 * <p>买家对已支付订单发起退款，运营审核；
 * 0 待审核 / 1 审核通过（订单 5→6）/ 2 审核驳回（订单 5→1）。
 * 由 RefundServiceImpl 读写。
 */
@Data
@TableName("t_refund")
public class Refund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号，对外唯一 */
    private String refundNo;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    /** 0 待审核 1 通过 2 驳回 */
    private Integer status;

    /** 审核备注 */
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
