package com.pyh.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运营端新增/编辑商品入参。
 * <p>由 AdminProductController 接收，AdminProductServiceImpl 在一个事务里写 t_product / t_sku / t_stock，
 * 并删除对应的商品详情缓存。
 */
@Data
public class ProductSaveReq {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 60, message = "商品名称最长 60 位")
    private String name;

    @Size(max = 120, message = "副标题最长 120 位")
    private String subtitle;

    @Size(max = 255, message = "封面地址最长 255 位")
    private String cover;

    @NotNull(message = "展示价不能为空")
    @DecimalMin(value = "0.01", message = "展示价必须大于 0")
    private BigDecimal price;

    /** SKU 列表，新增商品时至少一条 */
    @NotEmpty(message = "至少需要一个 SKU")
    @Valid
    private List<SkuSaveReq> skus;

    /**
     * SKU 入参：id 为空表示新增，有 id 表示更新。
     */
    @Data
    public static class SkuSaveReq {

        /** 已有 SKU 的 id，新增时为空 */
        private Long id;

        @NotBlank(message = "SKU 名称不能为空")
        @Size(max = 60, message = "SKU 名称最长 60 位")
        private String name;

        @NotNull(message = "SKU 价格不能为空")
        @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
        private BigDecimal price;

        /** 初始可用库存，新增时必填；更新时忽略（库存单独走改库存接口） */
        @Min(value = 0, message = "库存不能为负")
        private Integer stock;
    }
}
