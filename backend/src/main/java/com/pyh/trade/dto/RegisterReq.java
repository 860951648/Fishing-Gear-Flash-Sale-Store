package com.pyh.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册入参。
 * <p>由 AuthController#register 接收并 @Valid 校验，AuthServiceImpl 做用户名唯一性检查后写 t_user。
 */
@Data
public class RegisterReq {

    /** 登录名，4-20 位字母数字下划线 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需 4-20 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /** 明文密码，入库前 BCrypt 加密 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需 6-32 位")
    private String password;

    /** 昵称，可空 */
    @Size(max = 30, message = "昵称最长 30 位")
    private String nickname;

    /** 手机号，可空；填了就要符合 11 位手机号格式 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
