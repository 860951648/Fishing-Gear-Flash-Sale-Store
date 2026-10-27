package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.dto.PayCallbackReq;
import com.pyh.trade.entity.Order;
import com.pyh.trade.entity.PaymentRecord;
import com.pyh.trade.mapper.OrderMapper;
import com.pyh.trade.mapper.PaymentRecordMapper;
import com.pyh.trade.security.LoginUser;
import com.pyh.trade.service.PaymentService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.utils.SecurityUtils;
import com.pyh.trade.utils.SignUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 支付服务实现。
 * <p>入账链路（顺序即防线）：
 * <ol>
 *   <li>时间戳偏差校验：超过 5 分钟的回调直接拒绝，防重放；</li>
 *   <li>验签：HMAC-SHA256，签名不对一律 400，绝不入账；</li>
 *   <li>分布式锁 {@code SETNX trade:lock:pay:{orderNo}}：同一订单同一时刻只允许一个线程处理回调；</li>
 *   <li>幂等：channel_trade_no 已有支付流水 → 直接返回成功（重复回调不重复入账）；</li>
 *   <li>状态机 CAS：{@code UPDATE t_order SET status=1 WHERE order_no=? AND status=0}，影响 0 行说明已被处理；</li>
 *   <li>写支付流水（channel_trade_no 唯一索引兜住极端并发）；</li>
 *   <li>Lua 释放锁：只删自己的锁，避免误删。</li>
 * </ol>
 * 被 PayController 调用。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> unlockScript;
    private final ObjectMapper objectMapper;

    @Value("${app.pay.secret}")
    private String paySecret;

    @Value("${app.pay.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;

    public PaymentServiceImpl(OrderMapper orderMapper, PaymentRecordMapper paymentRecordMapper,
                              StringRedisTemplate stringRedisTemplate,
                              @Qualifier("unlockScript") DefaultRedisScript<Long> unlockScript,
                              ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.unlockScript = unlockScript;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(String orderNo) {
        Long userId = SecurityUtils.requireUserId();
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        LoginUser loginUser = SecurityUtils.requireUser();
        if (!order.getUserId().equals(userId) && !loginUser.hasAuthority("order:manage")) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能支付自己的订单");
        }
        if (order.getStatus() == null || order.getStatus() != Constants.ORDER_STATUS_UNPAID) {
            throw new BusinessException(ResultCode.CONFLICT, "订单当前状态不可支付");
        }
        // 模拟网关：本地生成渠道流水号 + 签名，然后走和真实回调完全相同的入账逻辑
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = IdGenerator.uuid();
        String amount = order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        PayCallbackReq req = new PayCallbackReq();
        req.setOrderNo(orderNo);
        req.setChannelTradeNo(IdGenerator.mockChannelTradeNo());
        req.setAmount(new BigDecimal(amount));
        req.setTimestamp(timestamp);
        req.setNonce(nonce);
        req.setSign(SignUtils.hmacSha256Hex(paySecret,
                SignUtils.buildPayContent(orderNo, amount, timestamp, nonce)));
        handleCallback(req);
        log.info("模拟支付完成 orderNo={} channelTradeNo={}", orderNo, req.getChannelTradeNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callback(PayCallbackReq req) {
        verifySign(req);
        handleCallback(req);
    }

    /**
     * 验签 + 防重放校验。
     */
    private void verifySign(PayCallbackReq req) {
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - req.getTimestamp()) > timestampToleranceSeconds) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "回调时间戳超出允许范围，拒绝处理");
        }
        String amount = req.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String expected = SignUtils.hmacSha256Hex(paySecret,
                SignUtils.buildPayContent(req.getOrderNo(), amount, req.getTimestamp(), req.getNonce()));
        if (!expected.equalsIgnoreCase(req.getSign())) {
            log.warn("支付回调验签失败 orderNo={}", req.getOrderNo());
            throw new BusinessException(ResultCode.PARAM_ERROR, "签名校验失败");
        }
    }

    /**
     * 真正的入账逻辑（mock 与真实回调共用）。
     * <p>注意：锁在事务提交前释放，极端情况下第二个线程可能读到未提交的状态；
     * 因此还有 channel_trade_no 唯一索引与订单状态 CAS 两道兜底，最终不会重复入账。
     */
    private void handleCallback(PayCallbackReq req) {
        String lockKey = RedisKeys.payLock(req.getOrderNo());
        String lockValue = IdGenerator.uuid();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(Constants.PAY_LOCK_SECONDS));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ResultCode.CONFLICT, "支付处理中，请稍后重试");
        }
        try {
            // 幂等闸门一：渠道流水号已入账 → 直接返回成功，不重复加钱
            Long existing = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getChannelTradeNo, req.getChannelTradeNo()));
            if (existing != null && existing > 0) {
                log.info("支付回调重复，已幂等返回 orderNo={} channelTradeNo={}",
                        req.getOrderNo(), req.getChannelTradeNo());
                return;
            }

            Order order = getByOrderNo(req.getOrderNo());
            if (order == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
            }
            BigDecimal callbackAmount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (order.getPayAmount().compareTo(callbackAmount) != 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "支付金额与订单金额不一致");
            }

            // 幂等闸门二：状态机 CAS 0 → 1
            int affected = orderMapper.markPaid(req.getOrderNo(), Constants.ORDER_STATUS_UNPAID, LocalDateTime.now());
            if (affected == 0) {
                Order latest = getByOrderNo(req.getOrderNo());
                if (latest != null && latest.getStatus() != null
                        && latest.getStatus() >= Constants.ORDER_STATUS_PAID
                        && latest.getStatus() != Constants.ORDER_STATUS_CLOSED) {
                    log.info("订单已支付，重复回调幂等返回 orderNo={}", req.getOrderNo());
                    return;
                }
                throw new BusinessException(ResultCode.CONFLICT, "订单状态已变更，支付失败");
            }

            // 幂等闸门三：唯一索引兜住 channel_trade_no 并发重复
            PaymentRecord record = new PaymentRecord();
            record.setOrderNo(req.getOrderNo());
            record.setChannelTradeNo(req.getChannelTradeNo());
            record.setAmount(callbackAmount);
            record.setStatus(1);
            record.setRawBody(toJson(req));
            paymentRecordMapper.insert(record);
            log.info("支付入账成功 orderNo={} amount={}", req.getOrderNo(), callbackAmount);
        } finally {
            // 只删自己加的锁
            stringRedisTemplate.execute(unlockScript, Collections.singletonList(lockKey), lockValue);
        }
    }

    private Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    private String toJson(PayCallbackReq req) {
        try {
            return objectMapper.writeValueAsString(req);
        } catch (JacksonException e) {
            return "{\"orderNo\":\"" + req.getOrderNo() + "\"}";
        }
    }
}
