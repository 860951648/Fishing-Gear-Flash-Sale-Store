package com.pyh.trade.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（HS256）。
 * <p>token 里只放 {@code userId(sub) / jti / ver} 三个身份信息，不放权限列表：
 * 权限每次请求由 PermissionService 从 Redis/DB 加载，改权限后立即生效。
 * 由 AuthServiceImpl 签发、JwtAuthenticationFilter 解析。
 */
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long expireMinutes;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expire-minutes:120}") long expireMinutes) {
        // HS256 要求密钥至少 256 bit，配置里已保证 32 字节以上
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    /**
     * 签发 token。
     *
     * @param userId  用户 id
     * @param jti     token 唯一 id（白名单 key）
     * @param version token 版本号
     * @return JWT 字符串
     */
    public String generate(Long userId, String jti, long version) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMinutes * 60 * 1000);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti)
                .claim("ver", version)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验签 token。
     *
     * @param token JWT 字符串
     * @return claims
     * @throws JwtException token 非法/过期
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * token 有效期（秒），用于设置白名单 key 的 TTL，两者必须一致，否则会出现「token 还在但白名单已过期」。
     *
     * @return 秒
     */
    public long getExpireSeconds() {
        return expireMinutes * 60;
    }
}
