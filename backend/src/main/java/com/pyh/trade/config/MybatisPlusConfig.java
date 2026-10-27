package com.pyh.trade.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>内联拦截器顺序不能反：分页拦截器负责改写 SQL 加 LIMIT，乐观锁拦截器负责在
 * {@code @Version} 实体的 updateById 上追加 version 条件。分开注册能避免两者互相影响。
 * 由 Spring 在启动时装配，全局生效。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页 + 乐观锁拦截器。
     *
     * @return 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页：MP 3.5.17 起 SQL 解析逻辑放在 mybatis-plus-jsqlparser 模块
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数，防止前端传 pageSize=100000 打爆内存
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        // 乐观锁：t_stock / t_seckill_sku / t_order 上带 version 字段
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
