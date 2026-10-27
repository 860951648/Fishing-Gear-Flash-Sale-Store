package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper。
 * <p>扣减/回滚都用「带条件的 UPDATE」而不是「先查后改」：
 * {@code available >= 1} 放在 WHERE 里，由 InnoDB 行锁保证并发下不会超卖，
 * 返回值 1 = 扣成功，0 = 库存不足（上层据此 409 或补偿 Redis）。
 */
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 下单扣减：可用减 1、锁定加 1（条件：可用库存足够）。
     *
     * @param skuId SKU id
     * @return 影响行数，0 表示库存不足
     */
    @Update("UPDATE t_stock SET available = available - 1, locked = locked + 1, update_time = NOW() "
            + "WHERE sku_id = #{skuId} AND available >= 1")
    int deductOne(@Param("skuId") Long skuId);

    /**
     * 关单/退款回滚：可用加 1、锁定减 1（条件：锁定库存足够，防重复回滚）。
     *
     * @param skuId SKU id
     * @return 影响行数，0 表示没有可回滚的锁定库存
     */
    @Update("UPDATE t_stock SET available = available + 1, locked = locked - 1, update_time = NOW() "
            + "WHERE sku_id = #{skuId} AND locked >= 1")
    int rollbackOne(@Param("skuId") Long skuId);

    /**
     * 运营端直接设置可用库存（总库存同步改为 可用 + 锁定）。
     *
     * @param skuId     SKU id
     * @param available 目标可用库存
     * @return 影响行数
     */
    @Update("UPDATE t_stock SET available = #{available}, total = #{available} + locked, update_time = NOW() "
            + "WHERE sku_id = #{skuId}")
    int updateAvailable(@Param("skuId") Long skuId, @Param("available") Integer available);
}
