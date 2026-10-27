package com.pyh.trade.security;

import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.service.PermissionService;
import com.pyh.trade.utils.RedisKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 * <p>链路：取 {@code Authorization: Bearer xxx} → 验签解析 userId/jti/ver → 查 Redis 白名单
 * {@code trade:auth:whitelist:{jti}} → 对比 {@code trade:auth:ver:{userId}} → 从 PermissionService 加载权限
 * → 构造 Authentication 放进 SecurityContext。
 * <p>为什么还要白名单：JWT 本身无法主动失效，登出/改密后靠删白名单或 INCR 版本号让旧 token 立刻作废。
 * 任何一步不通过都「静默放行到后续链路」，由 EntryPoint 统一回 401，避免过滤器里写多种响应格式。
 * Redis 不可用属于依赖故障，直接回 503，避免把「服务异常」误报成「登录失效」把用户踢下线。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils,
                                   StringRedisTemplate stringRedisTemplate,
                                   PermissionService permissionService,
                                   ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.stringRedisTemplate = stringRedisTemplate;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        Claims claims;
        try {
            claims = jwtUtils.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("token 非法或已过期：{}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        String jti = claims.getId();
        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            filterChain.doFilter(request, response);
            return;
        }
        long tokenVersion = claims.get("ver") == null ? 0L : ((Number) claims.get("ver")).longValue();

        try {
            // 白名单：登出即删除，token 立即失效
            Boolean whitelisted = stringRedisTemplate.hasKey(RedisKeys.authWhitelist(jti));
            if (!Boolean.TRUE.equals(whitelisted)) {
                filterChain.doFilter(request, response);
                return;
            }
            // 版本号：改密/禁用时 INCR，历史 token 全部作废
            String versionInRedis = stringRedisTemplate.opsForValue().get(RedisKeys.authVersion(userId));
            long currentVersion = versionInRedis == null ? 0L : Long.parseLong(versionInRedis);
            if (currentVersion != tokenVersion) {
                filterChain.doFilter(request, response);
                return;
            }

            List<String> authorities = permissionService.loadAuthorities(userId);
            if (authorities.isEmpty()) {
                // 权限为空说明用户被禁用或已删除，按未登录处理（会被 401 拦住）
                filterChain.doFilter(request, response);
                return;
            }
            LoginUser loginUser = new LoginUser(userId, jti, tokenVersion, authorities);
            List<SimpleGrantedAuthority> granted = authorities.stream().map(SimpleGrantedAuthority::new).toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, granted);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RedisConnectionFailureException ex) {
            log.error("Redis 不可用，认证链路降级", ex);
            SecurityContextHolder.clearContext();
            JsonAuthWriter.write(response, objectMapper, HttpStatus.SERVICE_UNAVAILABLE.value(),
                    ResultCode.SERVICE_UNAVAILABLE, "认证服务暂不可用，请稍后重试");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
