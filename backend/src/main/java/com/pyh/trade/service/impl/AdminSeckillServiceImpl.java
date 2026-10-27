package com.pyh.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.Constants;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.dto.ActivitySaveReq;
import com.pyh.trade.entity.SeckillActivity;
import com.pyh.trade.entity.SeckillSku;
import com.pyh.trade.entity.Sku;
import com.pyh.trade.mapper.SeckillActivityMapper;
import com.pyh.trade.mapper.SeckillSkuMapper;
import com.pyh.trade.mapper.SkuMapper;
import com.pyh.trade.service.AdminSeckillService;
import com.pyh.trade.utils.RedisKeys;
import com.pyh.trade.vo.ActivityVO;
import com.pyh.trade.vo.IdVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营端秒杀活动服务实现。
 * <p>创建活动时校验时间窗与 SKU 合法性；预热把 DB 秒杀库存刷进 Redis，
 * 这是活动能开抢的前置动作（未预热时下单链路会自动回填兜底）。
 * 被 AdminSeckillController 调用。
 */
@Service
public class AdminSeckillServiceImpl implements AdminSeckillService {

    private static final Logger log = LoggerFactory.getLogger(AdminSeckillServiceImpl.class);

    private final SeckillActivityMapper activityMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final SkuMapper skuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AdminSeckillServiceImpl(SeckillActivityMapper activityMapper, SeckillSkuMapper seckillSkuMapper,
                                   SkuMapper skuMapper, StringRedisTemplate stringRedisTemplate) {
        this.activityMapper = activityMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.skuMapper = skuMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<ActivityVO> list() {
        List<SeckillActivity> activities = activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .orderByDesc(SeckillActivity::getId));
        if (activities.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = activities.stream().map(SeckillActivity::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                        .in(SeckillSku::getActivityId, ids))
                .stream().collect(Collectors.groupingBy(SeckillSku::getActivityId, Collectors.counting()));
        return activities.stream().map(activity -> {
            ActivityVO vo = new ActivityVO();
            vo.setId(activity.getId());
            vo.setName(activity.getName());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setStatus(activity.getStatus());
            vo.setSkuCount(countMap.getOrDefault(activity.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IdVO create(ActivitySaveReq req) {
        if (req.getStartTime() == null || req.getEndTime() == null
                || !req.getStartTime().isBefore(req.getEndTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        SeckillActivity activity = new SeckillActivity();
        activity.setName(req.getName());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setStatus(Constants.ENABLED);
        activityMapper.insert(activity);

        for (ActivitySaveReq.ActivitySkuReq skuReq : req.getSkus()) {
            Sku sku = skuMapper.selectById(skuReq.getSkuId());
            if (sku == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在：" + skuReq.getSkuId());
            }
            // 唯一索引 (activity_id, sku_id) 兜住同一活动重复配置同一 SKU
            SeckillSku seckillSku = new SeckillSku();
            seckillSku.setActivityId(activity.getId());
            seckillSku.setSkuId(skuReq.getSkuId());
            seckillSku.setSeckillPrice(skuReq.getSeckillPrice().setScale(2, RoundingMode.HALF_UP));
            seckillSku.setStock(skuReq.getStock());
            seckillSku.setVersion(0);
            seckillSkuMapper.insert(seckillSku);
        }
        log.info("运营创建秒杀活动 activityId={} skuCount={}", activity.getId(), req.getSkus().size());
        return new IdVO(activity.getId());
    }

    @Override
    public void preheat(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        List<SeckillSku> seckillSkus = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId));
        if (seckillSkus.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该活动还没有配置秒杀 SKU");
        }
        for (SeckillSku seckillSku : seckillSkus) {
            // 预热是「以 DB 为准」覆盖 Redis：活动开始前运营可能改过库存，
            // 直接用 SET 覆盖可以纠正之前预扣/补偿留下的偏差
            int stock = seckillSku.getStock() == null ? 0 : seckillSku.getStock();
            stringRedisTemplate.opsForValue()
                    .set(RedisKeys.seckillStock(seckillSku.getId()), String.valueOf(stock));
            log.info("秒杀库存预热 seckillSkuId={} stock={}", seckillSku.getId(), stock);
        }
        log.info("活动预热完成 activityId={} skuCount={}", activityId, seckillSkus.size());
    }
}
