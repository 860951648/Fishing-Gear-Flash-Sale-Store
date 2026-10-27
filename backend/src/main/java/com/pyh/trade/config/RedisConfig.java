package com.pyh.trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis 配置。
 * <p>Spring Data Redis 4 已经自动装配了 StringRedisTemplate / RedisTemplate，业务代码统一用
 * StringRedisTemplate（key、value 都是字符串，redis-cli 里可直接看），所以这里只做一件有业务含义的事：
 * 把三个 Lua 脚本注册成 Bean（DefaultRedisScript），脚本文件放 resources/lua。
 * <p>为什么用 DefaultRedisScript 而不是字符串拼接：脚本内容由 Spring 读文件后缓存 SHA1，
 * 运行时只传 KEYS/ARGV，用 EVALSHA 复用脚本，既不拼字符串（避免注入）也省带宽。
 * 被 SeckillServiceImpl（预扣/限流）、ProductServiceImpl 与 PaymentServiceImpl（解锁）注入使用。
 */
@Configuration
public class RedisConfig {

    /**
     * 秒杀预扣脚本：原子判断「库存够不够 + 这个人是不是已经抢过」，再 DECR。
     *
     * @param script lua/seckill_deduct.lua
     * @return 脚本包装
     */
    @Bean("seckillDeductScript")
    public DefaultRedisScript<Long> seckillDeductScript(@Value("classpath:lua/seckill_deduct.lua") Resource script) {
        return buildScript(script);
    }

    /**
     * 分布式锁释放脚本：只有「锁的持有者」才能删锁，避免误删别人刚拿到的锁。
     *
     * @param script lua/unlock.lua
     * @return 脚本包装
     */
    @Bean("unlockScript")
    public DefaultRedisScript<Long> unlockScript(@Value("classpath:lua/unlock.lua") Resource script) {
        return buildScript(script);
    }

    /**
     * 令牌桶限流脚本：时间戳 + 令牌数存 Hash，按流逝时间补令牌。
     *
     * @param script lua/rate_limit.lua
     * @return 脚本包装
     */
    @Bean("rateLimitScript")
    public DefaultRedisScript<Long> rateLimitScript(@Value("classpath:lua/rate_limit.lua") Resource script) {
        return buildScript(script);
    }

    private DefaultRedisScript<Long> buildScript(Resource resource) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(resource);
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
