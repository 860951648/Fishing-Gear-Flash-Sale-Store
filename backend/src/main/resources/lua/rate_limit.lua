-- 令牌桶限流脚本（秒杀接口）
-- KEYS[1] 限流 key：trade:rate:{userId}:/api/seckill
-- ARGV[1] 桶容量 capacity
-- ARGV[2] 每秒补充令牌数 rate
-- ARGV[3] 当前时间戳（秒）
-- ARGV[4] key 过期秒数
-- 返回：1 放行 / 0 限流
--
-- 为什么用令牌桶而不是固定窗口计数：
--   固定窗口在窗口切换的瞬间可以打进 2 倍流量（临界问题）；令牌桶按流逝时间线性补充令牌，
--   既能限制平均速率，又能容忍短时突发（容量）。逻辑放 Lua 里保证「读令牌-扣令牌」原子。

local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(bucket[1])
local lastTs = tonumber(bucket[2])

if tokens == nil then
    -- 首次访问：桶是满的
    tokens = capacity
end
if lastTs == nil then
    lastTs = now
end

-- 按流逝的秒数补充令牌，但不超过容量
local delta = now - lastTs
if delta > 0 then
    tokens = math.min(capacity, tokens + delta * rate)
end

local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

redis.call('HMSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('EXPIRE', KEYS[1], ttl)
return allowed
