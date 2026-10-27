package com.pyh.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应。
 * <p>返回 token 与用户信息；同时带上权限码列表，前端路由守卫据此控制运营菜单可见性
 * （A 的前端 meta.roles 需要 ADMIN 角色，对应 authorities 里的 ROLE_ADMIN）。
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT，前端放 localStorage，后续请求带 Authorization: Bearer xxx */
    private String token;

    /** 固定 Bearer，方便前端拼接 */
    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    private UserVO user;

    /** 权限码列表，如 product:manage */
    private List<String> permissions;

    public LoginVO(String token, Long expiresIn, UserVO user, List<String> permissions) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
        this.permissions = permissions;
    }
}
