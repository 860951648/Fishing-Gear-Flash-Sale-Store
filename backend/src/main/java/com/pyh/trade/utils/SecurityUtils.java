package com.pyh.trade.utils;

import com.pyh.trade.common.BusinessException;
import com.pyh.trade.common.ResultCode;
import com.pyh.trade.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具。
 * <p>Controller / Service 不再从参数或 ThreadLocal 里取用户，统一从 Spring Security 上下文取，
 * 保证「谁能操作谁的数据」这类判断只有一个入口。由各 Service 调用。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录返回 null。
     *
     * @return 登录用户
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户，未登录抛 401。
     *
     * @return 登录用户
     */
    public static LoginUser requireUser() {
        LoginUser loginUser = currentUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户 id，未登录抛 401。
     *
     * @return 用户 id
     */
    public static Long requireUserId() {
        return requireUser().getUserId();
    }
}
