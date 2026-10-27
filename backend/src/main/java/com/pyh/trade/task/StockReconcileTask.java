package com.pyh.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyh.trade.common.Constants;
import com.pyh.trade.entity.SeckillSku;
import com.pyh.trade.entity.Stock;
import com.pyh.trade.mapper.SeckillSkuMapper;
import com.pyh.trade.mapper.StockMapper;
import com.pyh.trade.utils.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日库存对账任务。
 * <p>秒杀链路是「Redis 预扣 + DB 条件扣减 + 失败补偿」，理论上最终一致，
 * 但进程崩溃、Redis 抖动都可能让两边出现偏差（Redis 少了/多了几个库存）。
 * 这里每天凌晨把 DB 秒杀库存与 Redis 库存对一遍：以 DB 为准覆盖 Redis，并打印差异明细，
 * 便于人工核对是否存在「Redis 有货但 DB 已扣光」的超卖风险。
 * <p>只在活动仍在进行时对齐，避免把已结束活动重新灌上库存。
 */
@Component
public class StockReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(StockReconcileTask.class);

    private final StockMapper stockMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public StockReconcileTask(StockMapper stockMapper, SeckillSkuMapper seckillSkuMapper,
                              StringRedisTemplate stringRedisTemplate) {
        this.stockMapper = stockMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 每天 03:30 执行一次对账。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void reconcile() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>());
        long warnCount = stocks.stream()
                .filter(s -> s.getAvailable() != null && s.getAvailable() < Constants.STOCK_WARN_THRESHOLD)
                .count();
        int totalAvailable = stocks.stream()
                .mapToInt(s -> s.getAvailable() == null ? 0 : s.getAvailable()).sum();
        log.info("库存对账：SKU {} 个，可用合计 {}，低于阈值 {} 的 SKU {} 个",
                stocks.size(), totalAvailable, Constants.STOCK_WARN_THRESHOLD, warnCount);

        List<SeckillSku> seckillSkus = seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>());
        if (seckillSkus.isEmpty()) {
            log.info("库存对账：暂无秒杀 SKU 需要对齐");
            return;
        }
        Map<Long, String> redisStocks = seckillSkus.stream()
                .map(SeckillSku::getId)
                .collect(Collectors.toMap(id -> id,
                        id -> String.valueOf(stringRedisTemplate.opsForValue().get(RedisKeys.seckillStock(id)))));
        int fixed = 0;
        for (SeckillSku seckillSku : seckillSkus) {
            String dbStock = String.valueOf(seckillSku.getStock() == null ? 0 : seckillSku.getStock());
            String redisStock = redisStocks.get(seckillSku.getId());
            if (!dbStock.equals(redisStock)) {
                log.warn("秒杀库存不一致 seckillSkuId={} db={} redis={}，以 DB 为准重建",
                        seckillSku.getId(), dbStock, redisStock);
                stringRedisTemplate.opsForValue()
                        .set(RedisKeys.seckillStock(seckillSku.getId()), dbStock);
                fixed++;
            }
        }
        log.info("库存对账完成：秒杀 SKU {} 个，修正 {} 个", seckillSkus.size(), fixed);
    }
}
