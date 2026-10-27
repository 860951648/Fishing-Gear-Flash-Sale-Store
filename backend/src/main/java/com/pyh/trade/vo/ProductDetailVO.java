package com.pyh.trade.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情出参。
 * <p>来自商品详情缓存（Redis Hash：data + expireAt，逻辑过期），空值也会缓存 60 秒防穿透。
 * 由 ProductServiceImpl#detail 组装。
 */
@Data
public class ProductDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String subtitle;

    private String cover;

    private BigDecimal price;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** SKU 列表（含库存） */
    private List<SkuVO> skus;
}
