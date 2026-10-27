package com.pyh.trade.utils;

/**
 * Redis key 统一命名。
 * <p>全部以 {@code trade:} 前缀隔离，避免与同机其他项目（项目 B 用 {@code admin:}）撞 key；
 * 由 AuthServiceImpl、PermissionService、ProductServiceImpl、SeckillServiceImpl、PaymentServiceImpl 调用，
 * 同一套拼法保证「写入方」与「读取方」不会写错 key。
 */
public final class RedisKeys {

    private static final String PREFIX = "trade:";

    private RedisKeys() {
    }

    /**
     * Token 白名单：登录时写入，登出时删除，JWT 过滤器每次请求校验。
     *
     * @param jti JWT 唯一 id
     * @return key
     */
    public static String authWhitelist(String jti) {
        return PREFIX + "auth:whitelist:" + jti;
    }

    /**
     * Token 版本号：改密/禁用时 INCR，使旧 token 全部失效。
     *
     * @param userId 用户 id
     * @return key
     */
    public static String authVersion(Long userId) {
        return PREFIX + "auth:ver:" + userId;
    }

    /**
     * 用户权限缓存（逗号分隔的权限码）。
     *
     * @param userId 用户 id
     * @return key
     */
    public static String permissions(Long userId) {
        return PREFIX + "perm:" + userId;
    }

    /**
     * 商品详情缓存（Hash：data / expireAt）。
     *
     * @param productId 商品 id
     * @return key
     */
    public static String productDetail(Long productId) {
        return PREFIX + "product:" + productId;
    }

    /**
     * 商品详情重建锁。
     *
     * @param productId 商品 id
     * @return key
     */
    public static String productLock(Long productId) {
        return PREFIX + "product:lock:" + productId;
    }

    /**
     * 秒杀库存（预热时写入，Lua 里 DECR）。
     *
     * @param seckillSkuId 秒杀 SKU id
     * @return key
     */
    public static String seckillStock(Long seckillSkuId) {
        return PREFIX + "seckill:stock:" + seckillSkuId;
    }

    /**
     * 秒杀用户幂等标记，与 t_order 的 uk_user_seckill 双保险。
     *
     * @param seckillSkuId 秒杀 SKU id
     * @param userId       用户 id
     * @return key
     */
    public static String seckillUser(Long seckillSkuId, Long userId) {
        return PREFIX + "seckill:user:" + seckillSkuId + ":" + userId;
    }

    /**
     * 支付回调分布式锁。
     *
     * @param orderNo 订单号
     * @return key
     */
    public static String payLock(String orderNo) {
        return PREFIX + "lock:pay:" + orderNo;
    }

    /**
     * 秒杀接口令牌桶限流 key。
     *
     * @param userId 用户 id
     * @param path   接口路径
     * @return key
     */
    public static String rateLimit(Long userId, String path) {
        return PREFIX + "rate:" + userId + ":" + path;
    }
}
