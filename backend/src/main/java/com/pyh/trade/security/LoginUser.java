package com.pyh.trade.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 登录用户上下文对象。
 * <p>由 JwtAuthenticationFilter 解析 token 后构造，放进 SecurityContext；
 * 业务层通过 SecurityUtils 取 userId，方法级 @PreAuthorize 通过 authorities 判定权限。
 * 只放「身份 + 本次会话」信息，不缓存整张用户表。
 */
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 id */
    private final Long userId;
    /** JWT 唯一 id，登出时按它删白名单 */
    private final String jti;
    /** token 版本号，与 Redis auth:ver:{userId} 对比 */
    private final long version;
    /** 权限码列表（如 product:manage），每次请求从 Redis/DB 加载 */
    private final List<String> authorities;

    public LoginUser(Long userId, String jti, long version, List<String> authorities) {
        this.userId = userId;
        this.jti = jti;
        this.version = version;
        this.authorities = authorities == null ? Collections.emptyList() : List.copyOf(authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public String getJti() {
        return jti;
    }

    public long getVersion() {
        return version;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    /**
     * 判断是否拥有某个权限（用于业务内联判断，如「只能看自己的订单」）。
     *
     * @param authority 权限码
     * @return 是否拥有
     */
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
