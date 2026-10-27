package com.pyh.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 运营端调整 SKU 可用库存入参。
 * <p>由 AdminProductController#updateStock 接收，AdminProductServiceImpl 直接覆盖 available
 * 并同步 total = available + locked，同时删商品详情缓存。
 */
@Data
public class StockUpdateReq {

    @NotNull(message = "可用库存不能为空")
    @Min(value = 0, message = "可用库存不能为负")
    private Integer available;
}
