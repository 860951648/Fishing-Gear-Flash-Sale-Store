package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项出参。
 * <p>由商品列表、运营商品列表共用；价格取 t_product.price（SKU 最低价），保留两位小数。
 */
@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String subtitle;

    private String cover;

    private BigDecimal price;

    /** 1 上架 / 0 下架 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
