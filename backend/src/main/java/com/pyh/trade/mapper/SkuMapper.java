package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.Sku;

/**
 * SKU Mapper。
 * <p>商品详情按 productId 查全部规格，秒杀配置时按 id 校验 SKU 是否存在。
 */
public interface SkuMapper extends BaseMapper<Sku> {
}
