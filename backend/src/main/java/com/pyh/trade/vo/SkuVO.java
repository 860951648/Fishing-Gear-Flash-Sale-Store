package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU 出参。
 * <p>商品详情里每个 SKU 带可用库存（来自 t_stock.available），前端显示「剩余 N 件」。
 */
@Data
public class SkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private BigDecimal price;

    /** 可用库存 */
    private Integer available;

    /** 锁定库存 */
    private Integer locked;
}
