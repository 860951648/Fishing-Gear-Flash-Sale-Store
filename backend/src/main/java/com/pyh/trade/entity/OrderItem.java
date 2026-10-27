package com.pyh.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体（t_order_item）。
 * <p>下单时把商品名/SKU 名/价格冗余进来，商品后续改价改名不影响历史订单展示。
 * 由订单创建（OrderServiceImpl）写入、订单详情读取。
 */
@Data
@TableName("t_order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long skuId;

    /** 商品名快照 */
    private String productName;

    /** SKU 名快照 */
    private String skuName;

    /** 成交单价 */
    private BigDecimal price;

    private Integer quantity;
}
