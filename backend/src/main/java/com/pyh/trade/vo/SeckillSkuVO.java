package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 活动内秒杀 SKU 出参。
 * <p>{@code redisStock} 是 Redis 里的实时预扣库存（买家看到的秒杀剩余量），
 * {@code dbStock} 是 t_seckill_sku.stock（落库值）；两者短暂不一致是正常现象，
 * 预热接口会把 dbStock 刷成 redisStock。
 */
@Data
public class SeckillSkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 秒杀 SKU 主键，下单时作为路径里的 skuId */
    private Long seckillSkuId;

    /** 真实 SKU id */
    private Long skuId;

    private String skuName;

    private String productName;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** Redis 实时库存，未预热时回落为 DB 库存 */
    private Integer redisStock;

    /** DB 库存 */
    private Integer dbStock;
}
