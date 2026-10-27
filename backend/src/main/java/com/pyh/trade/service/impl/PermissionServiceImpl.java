package com.pyh.trade.service.impl;

import com.pyh.trade.entity.User;
import com.pyh.trade.mapper.UserMapper;
import com.pyh.trade.service.PermissionService;
import com.pyh.trade.utils.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 权限服务实现。
 * <p>A 项目只有 t_user，没有角色/权限表，所以权限按账号规则推导：{@code admin} 是运营账号，
 * 拥有 5 个管理权限码；其他账号只有 ROLE_USER（可访问登录态接口）。
 * <p>为什么缓存到 Redis：JWT 不带权限列表，每个请求都要判定权限，若每次都查库会给 MySQL 增加无谓压力；
 * 缓存 5 分钟，权限变更时删缓存即可立即生效。
 * 被 JwtAuthenticationFilter（每请求）与 AuthServiceImpl（登录）调用。
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    /** 运营账号名 */
    private static final String ADMIN_USERNAME = "admin";

    /** 运营权限码：与 @PreAuthorize 中使用的字符串严格一致 */
    private static final List<String> ADMIN_AUTHORITIES = List.of(
            "ROLE_ADMIN", "ROLE_USER",
            "product:manage", "seckill:manage", "order:manage", "refund:audit", "stats:view");

    /** 普通买家权限码 */
    private static final List<String> BUYER_AUTHORITIES = List.of("ROLE_USER");

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public PermissionServiceImpl(UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<String> loadAuthorities(Long userId) {
        String cacheKey = RedisKeys.permissions(userId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // 空串代表「查过了，没有权限」，避免缓存穿透到库
            return cached.isEmpty() ? Collections.emptyList() : Arrays.asList(cached.split(","));
        }
        List<String> authorities = queryFromDb(userId);
        stringRedisTemplate.opsForValue()
                .set(cacheKey, String.join(",", authorities), Duration.ofSeconds(300));
        return authorities;
    }

    @Override
    public void evictPermissions(Long userId) {
        stringRedisTemplate.delete(RedisKeys.permissions(userId));
    }

    @Override
    public long currentVersion(Long userId) {
        String key = RedisKeys.authVersion(userId);
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            // 首次登录初始化版本号，后续 INCR 才有基线
            stringRedisTemplate.opsForValue().setIfAbsent(key, "0");
            return 0L;
        }
        return Long.parseLong(value);
    }

    @Override
    public void bumpVersion(Long userId) {
        // 版本号 +1 后，旧 token 里的 ver 与 Redis 不一致，过滤器直接判定失效
        stringRedisTemplate.opsForValue().increment(RedisKeys.authVersion(userId));
        evictPermissions(userId);
        log.info("用户 {} token 版本号已递增，旧 token 全部失效", userId);
    }

    private List<String> queryFromDb(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return Collections.emptyList();
        }
        return ADMIN_USERNAME.equals(user.getUsername()) ? ADMIN_AUTHORITIES : BUYER_AUTHORITIES;
    }
}
