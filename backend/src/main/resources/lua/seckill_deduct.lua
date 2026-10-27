-- 秒杀预扣脚本
-- KEYS[1] 秒杀库存 key：trade:seckill:stock:{seckillSkuId}
-- KEYS[2] 用户幂等 key：trade:seckill:user:{seckillSkuId}:{userId}
-- ARGV[1] 幂等 key 的 value（写入下单时间戳，便于排查）
-- ARGV[2] 幂等 key 过期秒数
-- 返回：1 预扣成功 / 0 库存不足 / 2 重复下单 / -1 未预热或库存值异常
--
-- 为什么把「查库存 + 判重复 + 扣减」写在一个脚本里：
--   三段逻辑如果分开调用，两个并发请求可能都读到 stock=1 然后各自 DECR，最终扣成 -1（超卖）。
--   Redis 单线程执行 Lua，整段脚本原子完成，天然不会超卖；同时用 SETNX 做「一人一单」，
--   比数据库唯一索引更早挡住重复请求，避免无谓的 DB 事务。
-- 为什么 -1 既代表「没预热」也代表「值不是数字」：
--   tonumber 失败时直接比较会抛 "attempt to compare nil with number"，整条链路变成 500。
--   把脏数据交给 Java 侧用 DB 库存强制覆盖（预热/对账都以 DB 为准），比让用户看到 500 更合理。

local available = tonumber(redis.call('GET', KEYS[1]))
if available == nil then
    -- 未预热，或 key 里是脏数据（非数字）：由 Java 侧回填后重试一次
    return -1
end

if available <= 0 then
    return 0
end

-- SETNX 成功返回 1，说明这是该用户第一次抢这个 SKU
if redis.call('SETNX', KEYS[2], ARGV[1]) == 0 then
    return 2
end

-- 幂等标记必须有过期时间，否则回滚/补偿漏删会导致该用户永远无法再买
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))
redis.call('DECR', KEYS[1])
return 1
