package com.pyh.trade.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 签名工具。
 * <p>支付回调签名串为 {@code orderNo|amount|timestamp|nonce}，用 {@code app.pay.secret} 做 HMAC-SHA256，
 * 输出小写十六进制。双方用同一套拼法和密钥，改任何一位都会导致验签失败。
 * 由 PaymentServiceImpl 生成（mockPay）与校验（callback）使用。
 */
public final class SignUtils {

    private static final String ALGORITHM = "HmacSHA256";

    private SignUtils() {
    }

    /**
     * 计算签名。
     *
     * @param secret  密钥
     * @param content 待签名串
     * @return 小写十六进制签名
     */
    public static String hmacSha256Hex(String secret, String content) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 初始化失败", e);
        }
    }

    /**
     * 拼装支付回调签名串。
     *
     * @param orderNo   订单号
     * @param amount    金额（元，两位小数）
     * @param timestamp 秒级时间戳
     * @param nonce     随机串
     * @return 签名串
     */
    public static String buildPayContent(String orderNo, String amount, long timestamp, String nonce) {
        return orderNo + "|" + amount + "|" + timestamp + "|" + nonce;
    }
}
