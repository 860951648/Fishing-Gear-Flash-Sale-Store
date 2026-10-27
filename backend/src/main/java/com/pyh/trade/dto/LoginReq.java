package com.pyh.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参。
 * <p>由 AuthController#login 接收，AuthServiceImpl 用 BCrypt 校验密码后签发 JWT。
 */
@Data
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
