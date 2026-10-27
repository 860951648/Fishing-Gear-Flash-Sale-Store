package com.pyh.trade.service;

import com.pyh.trade.dto.LoginReq;
import com.pyh.trade.dto.RegisterReq;
import com.pyh.trade.vo.LoginVO;
import com.pyh.trade.vo.UserVO;

/**
 * 认证服务。
 * <p>负责注册、登录（签发 JWT + 写白名单）、登出（删白名单）、查询当前用户。
 * 由 AuthController 调用；权限码由 {@link PermissionService} 提供。
 */
public interface AuthService {

    /**
     * 注册买家账号。
     *
     * @param req 注册入参
     */
    void register(RegisterReq req);

    /**
     * 登录并签发 token。
     *
     * @param req 登录入参
     * @return token + 用户信息 + 权限码
     */
    LoginVO login(LoginReq req);

    /**
     * 登出：删除当前 token 的白名单 key。
     */
    void logout();

    /**
     * 查询当前登录用户。
     *
     * @return 用户信息
     */
    UserVO me();
}
