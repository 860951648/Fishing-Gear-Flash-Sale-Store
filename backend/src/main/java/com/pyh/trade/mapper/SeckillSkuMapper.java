package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.SeckillSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀 SKU Mapper。
 * <p>Redis 预扣成功后才调用这里的条件扣减：{@code stock >= 1} 保证 DB 库存不会被扣成负数；
 * 影响 0 行说明 Redis 与 DB 出现偏差（例如预热前被手工改过 DB），上层会补偿 Redis。
 */
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {

    /**
     * 条件扣减秒杀库存。
     *
     * @param id 秒杀 SKU 主键
     * @return 影响行数，0 表示库存不足
     */
    @Update("UPDATE t_seckill_sku SET stock = stock - 1, version = version + 1, update_time = NOW() "
            + "WHERE id = #{id} AND stock >= 1")
    int deductStock(@Param("id") Long id);

    /**
     * 关单/取消时把秒杀库存补回去。
     *
     * @param id 秒杀 SKU 主键
     * @return 影响行数
     */
    @Update("UPDATE t_seckill_sku SET stock = stock + 1, version = version + 1, update_time = NOW() "
            + "WHERE id = #{id}")
    int restoreStock(@Param("id") Long id);
}
