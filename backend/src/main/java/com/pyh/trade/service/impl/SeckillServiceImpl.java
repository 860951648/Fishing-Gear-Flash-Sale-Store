package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.entity.Product;
import com.pyh.trade.entity.SeckillActivity;
import com.pyh.trade.entity.SeckillSku;
import com.pyh.trade.entity.Sku;
import com.pyh.trade.mapper.ProductMapper;
import com.pyh.trade.mapper.SeckillActivityMapper;
import com.pyh.trade.mapper.SeckillSkuMapper;
import com.pyh.trade.mapper.SkuMapper;
import com.pyh.trade.service.OrderService;
import com.pyh.trade.service.SeckillService;
import com.pyh.trade.utils.IdGenerator;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.utils.SecurityUtils;
import com.pyh.trade.vo.ActivityDetailVO;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.OrderNoVO;
import com.pyh.trade.vo.SeckillSkuVO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 秒杀服务实现。
 * <p>下单链路（每一层都在挡流量，越靠前的成本越低）：
 * <ol>
 *   <li>Lua 令牌桶限流：拦住脚本刷接口，key 维度是「用户 + 接口」；</li>
 *   <li>Lua 原子预扣：一次性完成「判库存 + 判重复 + DECR」，杜绝超卖；</li>
 *   <li>同步 DB 事务：条件 UPDATE 两个库存 + 写订单/明细/流水（不依赖 MQ，MQ 没装也能下单）；</li>
 *   <li>失败补偿：DB 事务抛异常时把 Redis 库存加回去、删幂等标记。</li>
 * </ol>
 * 这样设计的原因：Redis 扣减快但不可靠（会丢数据/宕机），DB 可靠但扛不住高并发，
 * 用 Redis 做「漏斗」、DB 做「账本」，两者用条件扣减 + 补偿保证最终一致。
 * 被 SeckillController 调用。
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    private final SeckillActivityMapper activityMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillDeductScript;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final com.pyh.trade.mq.producer.OrderDelayProducer orderDelayProducer;

    @Value("${app.seckill.rate-limit-capacity:20}")
    private int rateLimitCapacity;

    @Value("${app.seckill.rate-limit-rate:10}")
    private int rateLimitRate;

    public SeckillServiceImpl(SeckillActivityMapper activityMapper, SeckillSkuMapper seckillSkuMapper,
                              SkuMapper skuMapper, ProductMapper productMapper, OrderService orderService,
                              StringRedisTemplate stringRedisTemplate,
                              @Qualifier("seckillDeductScript") DefaultRedisScript<Long> seckillDeductScript,
                              @Qualifier("rateLimitScript") DefaultRedisScript<Long> rateLimitScript,
                              com.pyh.trade.mq.producer.OrderDelayProducer orderDelayProducer) {
        this.activityMapper = activityMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.orderService = orderService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillDeductScript = seckillDeductScript;
        this.rateLimitScript = rateLimitScript;
        this.orderDelayProducer = orderDelayProducer;
    }

    @PostConstruct
    public void init() {
        log.info("秒杀服务就绪：限流容量 {}，补充速率 {}/s", rateLimitCapacity, rateLimitRate);
    }

    @Override
    public List<ActivityVO> listActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, Constants.ENABLED)
                .gt(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime));
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = activities.stream().map(SeckillActivity::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                        .in(SeckillSku::getActivityId, ids))
                .stream().collect(Collectors.groupingBy(SeckillSku::getActivityId, Collectors.counting()));
        List<ActivityVO> result = new ArrayList<>(activities.size());
        for (SeckillActivity activity : activities) {
            result.add(toActivityVO(activity, countMap.getOrDefault(activity.getId(), 0L).intValue()));
        }
        return result;
    }

    @Override
    public ActivityDetailVO detail(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        List<SeckillSku> seckillSkus = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId)
                .orderByAsc(SeckillSku::getId));
        List<SeckillSkuVO> skuVOs = new ArrayList<>(seckillSkus.size());
        for (SeckillSku seckillSku : seckillSkus) {
            skuVOs.add(toSeckillSkuVO(seckillSku));
        }
        LocalDateTime now = LocalDateTime.now();
        ActivityDetailVO vo = new ActivityDetailVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setInProgress(activity.getStatus() == Constants.ENABLED
                && !now.isBefore(activity.getStartTime()) && !now.isAfter(activity.getEndTime()));
        vo.setSkus(skuVOs);
        return vo;
    }

    @Override
    public OrderNoVO seckill(Long activityId, Long skuId) {
        Long userId = SecurityUtils.requireUserId();

        // ① 限流：桶容量 20、每秒补 10 个，超出直接 429，别让刷子走进后面更贵的逻辑
        if (!allowRequest(userId)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }

        // ② 活动与 SKU 合法性
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() == null || activity.getStatus() != Constants.ENABLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动已下线");
        }
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动已结束");
        }
        SeckillSku seckillSku = seckillSkuMapper.selectOne(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId)
                .eq(SeckillSku::getSkuId, skuId));
        if (seckillSku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动下不存在该 SKU");
        }

        // ③ Redis 原子预扣（未预热时回填一次再重试，避免运营漏点预热就整场卖不动）
        String stockKey = RedisKeys.seckillStock(seckillSku.getId());
        String userKey = RedisKeys.seckillUser(seckillSku.getId(), userId);
        Long deductResult = executeDeduct(stockKey, userKey);
        if (deductResult != null && deductResult == -1L) {
            preloadStock(seckillSku);
            deductResult = executeDeduct(stockKey, userKey);
        }
        if (deductResult == null) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "秒杀失败，请稍后重试");
        }
        if (deductResult == -1L) {
            // 回填后仍然读不到数字，说明 Redis 不可写或库存被别的逻辑改成脏值，给明确提示而不是 500
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "秒杀库存未就绪，请稍后重试");
        }
        if (deductResult == 0L) {
            throw new BusinessException(ResultCode.CONFLICT, "手慢了，秒杀库存已抢完");
        }
        if (deductResult == 2L) {
            throw new BusinessException(ResultCode.CONFLICT, "您已抢到过该商品，请勿重复下单");
        }

        // ④ 同步落库；任何异常都补偿 Redis，保证「Redis 扣了但 DB 没成」不会白扣库存
        try {
            String orderNo = orderService.createSeckillOrder(seckillSku, activityId, userId);
            log.info("秒杀成功 activityId={} skuId={} userId={} orderNo={}", activityId, skuId, userId, orderNo);
            // 事务提交后再投递延迟关单消息（MQ 未启用时生产者在内部降级为定时任务）
            orderDelayProducer.sendCloseDelayMessage(orderNo);
            return new OrderNoVO(orderNo);
        } catch (RuntimeException e) {
            compensate(stockKey, userKey);
            log.warn("秒杀落库失败，已补偿 Redis 预扣 activityId={} skuId={} userId={} cause={}",
                    activityId, skuId, userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 执行预扣脚本，返回 1 成功 / 0 库存不足 / 2 重复 / -1 未预热。
     */
    private Long executeDeduct(String stockKey, String userKey) {
        List<String> keys = new ArrayList<>(2);
        keys.add(stockKey);
        keys.add(userKey);
        return stringRedisTemplate.execute(seckillDeductScript, keys,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(Constants.SECKILL_IDEMPOTENT_SECONDS));
    }

    /**
     * 预热兜底：Redis 没有库存 key（或被写成非数字的脏数据）时用 DB 库存回填。
     * 正常缺失用 SETNX，避免并发回填把别人已经扣过的库存覆盖回去；
     * 脏数据必须用 SET 强制覆盖，否则 SETNX 永远不会成功，活动整场都卖不出去。
     */
    private void preloadStock(SeckillSku seckillSku) {
        String stockKey = RedisKeys.seckillStock(seckillSku.getId());
        int stock = seckillSku.getStock() == null ? 0 : seckillSku.getStock();
        String current = stringRedisTemplate.opsForValue().get(stockKey);
        if (current == null) {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(stock));
            if (Boolean.TRUE.equals(success)) {
                log.info("秒杀库存未预热，已自动回填 seckillSkuId={} stock={}", seckillSku.getId(), stock);
            }
            return;
        }
        try {
            Integer.parseInt(current);
        } catch (NumberFormatException e) {
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
            log.warn("秒杀库存为脏数据，已按 DB 库存覆盖 seckillSkuId={} dirty='{}' stock={}",
                    seckillSku.getId(), current, stock);
        }
    }

    /**
     * 补偿 Redis 预扣：库存 +1，删除用户幂等标记（否则该用户 24 小时内都抢不了）。
     */
    private void compensate(String stockKey, String userKey) {
        try {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userKey);
        } catch (RuntimeException ex) {
            // 补偿失败只记录：Redis 库存会由预热/对账任务用 DB 库存覆盖
            log.error("Redis 预扣补偿失败 stockKey={}", stockKey, ex);
        }
    }

    /**
     * 令牌桶限流，返回 true 表示放行。
     */
    private boolean allowRequest(Long userId) {
        String key = RedisKeys.rateLimit(userId, "/api/seckill");
        Long allowed = stringRedisTemplate.execute(rateLimitScript, Collections.singletonList(key),
                String.valueOf(rateLimitCapacity), String.valueOf(rateLimitRate),
                String.valueOf(System.currentTimeMillis() / 1000),
                String.valueOf(Constants.RATE_LIMIT_TTL));
        return allowed != null && allowed == 1L;
    }

    private ActivityVO toActivityVO(SeckillActivity activity, int skuCount) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setSkuCount(skuCount);
        return vo;
    }

    private SeckillSkuVO toSeckillSkuVO(SeckillSku seckillSku) {
        SeckillSkuVO vo = new SeckillSkuVO();
        vo.setSeckillSkuId(seckillSku.getId());
        vo.setSkuId(seckillSku.getSkuId());
        vo.setSeckillPrice(seckillSku.getSeckillPrice());
        vo.setDbStock(seckillSku.getStock());
        Sku sku = skuMapper.selectById(seckillSku.getSkuId());
        if (sku != null) {
            vo.setSkuName(sku.getName());
            vo.setOriginalPrice(sku.getPrice());
            Product product = productMapper.selectById(sku.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
            }
        }
        // 实时库存优先取 Redis（预扣后立即变化），没预热则回落 DB
        String redisValue = stringRedisTemplate.opsForValue().get(RedisKeys.seckillStock(seckillSku.getId()));
        if (redisValue == null) {
            vo.setRedisStock(seckillSku.getStock());
        } else {
            try {
                vo.setRedisStock(Integer.parseInt(redisValue));
            } catch (NumberFormatException e) {
                vo.setRedisStock(seckillSku.getStock());
            }
        }
        return vo;
    }
}
