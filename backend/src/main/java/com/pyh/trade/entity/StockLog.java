package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水实体（t_stock_log）。
 * <p>每次库存变动留痕，{@code (order_no, type)} 唯一索引保证同一订单同一动作只回滚一次
 * （关单与退款并发时不会重复加库存）。由 OrderServiceImpl / RefundServiceImpl 写入。
 */
@Data
@TableName("t_stock_log")
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long skuId;

    /** 1 下单扣减 / 2 退款回滚 / 3 关单回滚 */
    private Integer type;

    private Integer quantity;

    private LocalDateTime createTime;
}
