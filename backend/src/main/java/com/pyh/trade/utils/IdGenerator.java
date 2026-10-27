package com.pyh.trade.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成器。
 * <p>订单号 / 退款单号 / 模拟渠道流水号都在这里生成：
 * 「前缀 + 秒级时间戳 + 6 位随机数」，既方便肉眼定位下单时间，又能靠数据库唯一索引兜住极小概率的碰撞。
 * 由 OrderServiceImpl、RefundServiceImpl、PaymentServiceImpl 调用。
 */
public final class IdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private IdGenerator() {
    }

    /**
     * 订单号：SO20261027183025123456。
     *
     * @return 订单号
     */
    public static String orderNo() {
        return "SO" + LocalDateTime.now().format(FORMATTER) + randomSuffix();
    }

    /**
     * 退款单号：RF20261027183025123456。
     *
     * @return 退款单号
     */
    public static String refundNo() {
        return "RF" + LocalDateTime.now().format(FORMATTER) + randomSuffix();
    }

    /**
     * 本地模拟支付网关的渠道交易号：MOCK + 时间戳 + 随机数。
     *
     * @return 渠道交易号
     */
    public static String mockChannelTradeNo() {
        return "MOCK" + LocalDateTime.now().format(FORMATTER) + randomSuffix();
    }

    /**
     * 通用唯一标识（消息 id、锁持有者标识、随机 nonce）。
     *
     * @return 32 位十六进制字符串
     */
    public static String uuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private static String randomSuffix() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
