-- 分布式锁释放脚本
-- KEYS[1] 锁 key（如 trade:lock:pay:{orderNo}）
-- ARGV[1] 加锁时写入的持有者标识（UUID）
-- 返回：1 删除成功 / 0 锁已不属于自己
--
-- 为什么不能直接 DEL：
--   线程 A 的锁可能已超时释放、线程 B 刚拿到锁，此时 A 再 DEL 就会把 B 的锁删掉，
--   导致同一订单出现两个支付处理线程。所以必须「先比对持有者再删除」，比对和删除要原子。

if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
