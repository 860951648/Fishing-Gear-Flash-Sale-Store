package com.pyh.trade.common;

/**
 * 全局常量字典。
 * <p>状态机取值、库存流水类型、分页上限等散落各处的“魔法数字”统一收敛到这里，
 * 被 Service / Task / Mapper 层引用。
 */
public final class Constants {

    private Constants() {
    }

    /* ===================== 订单状态机 ===================== */
    /** 待支付 */
    public static final int ORDER_STATUS_UNPAID = 0;
    /** 已支付 */
    public static final int ORDER_STATUS_PAID = 1;
    /** 已发货 */
    public static final int ORDER_STATUS_SHIPPED = 2;
    /** 已完成 */
    public static final int ORDER_STATUS_FINISHED = 3;
    /** 已关闭（超时/主动取消） */
    public static final int ORDER_STATUS_CLOSED = 4;
    /** 退款中 */
    public static final int ORDER_STATUS_REFUNDING = 5;
    /** 已退款 */
    public static final int ORDER_STATUS_REFUNDED = 6;

    /* ===================== 退款单状态 ===================== */
    /** 待审核 */
    public static final int REFUND_STATUS_WAIT = 0;
    /** 审核通过 */
    public static final int REFUND_STATUS_PASS = 1;
    /** 审核驳回 */
    public static final int REFUND_STATUS_REJECT = 2;

    /* ===================== 库存流水类型 ===================== */
    /** 下单扣减 */
    public static final int STOCK_LOG_DEDUCT = 1;
    /** 退款回滚 */
    public static final int STOCK_LOG_REFUND = 2;
    /** 超时/取消关单回滚 */
    public static final int STOCK_LOG_CLOSE = 3;

    /* ===================== 活动/商品状态 ===================== */
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    /* ===================== 通用 ===================== */
    /** 商品库存预警阈值 */
    public static final int STOCK_WARN_THRESHOLD = 10;
    /** 分页最大条数，防止 pageSize 被放大拖垮数据库 */
    public static final int MAX_PAGE_SIZE = 100;
    /** 订单超时时长（分钟），两种关单路径共用 */
    public static final int ORDER_TIMEOUT_MINUTES = 30;
    /** 关单扫描批量大小 */
    public static final int CLOSE_ORDER_BATCH = 100;
    /** 商品详情逻辑过期时间（分钟） */
    public static final int PRODUCT_CACHE_MINUTES = 5;
    /** 空值缓存时间（秒），防穿透 */
    public static final int NULL_CACHE_SECONDS = 60;
    /** 秒杀幂等 key 过期时间（秒） */
    public static final int SECKILL_IDEMPOTENT_SECONDS = 24 * 60 * 60;
    /** 支付回调分布式锁过期时间（秒） */
    public static final int PAY_LOCK_SECONDS = 10;
    /** 秒杀限流：桶容量 */
    public static final int RATE_LIMIT_CAPACITY = 20;
    /** 秒杀限流：令牌恢复速率（个/秒） */
    public static final int RATE_LIMIT_RATE = 10;
    /** 限流 key 过期时间（秒） */
    public static final int RATE_LIMIT_TTL = 60;
    /** 权限缓存时间（秒） */
    public static final int PERM_CACHE_SECONDS = 5 * 60;
}
