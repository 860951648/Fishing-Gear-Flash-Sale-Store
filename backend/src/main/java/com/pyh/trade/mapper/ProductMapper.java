package com.pyh.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pyh.trade.entity.Product;

/**
 * 商品表 Mapper。
 * <p>分页查询走 MyBatis-Plus 条件构造器；逻辑删除由 {@code @TableLogic} 自动追加条件。
 */
public interface ProductMapper extends BaseMapper<Product> {
}
