package com.pyh.trade.controller;

import com.pyh.trade.common.Result;
import com.pyh.trade.dto.LoginReq;
import com.pyh.trade.dto.RegisterReq;
import com.pyh.trade.service.AuthService;
import com.pyh.trade.vo.LoginVO;
import com.pyh.trade.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（接口契约 §3.3 第 1-4 个）。
 * <p>只做参数校验与转发，业务在 AuthServiceImpl；注册/登录是公开接口，登出/me 需要登录。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册买家账号 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        authService.register(req);
        return Result.ok();
    }

    /** 登录，返回 token 与权限码 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    /** 登出：删除当前 token 的白名单 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }
}
