package com.pyh.trade.security;

import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限处理器。
 * <p>已登录但缺少权限码（过滤器链层面判定为匿名/表达式拒绝）时返回 403 + {@code Result} JSON。
 * 方法级 @PreAuthorize 抛出的 AccessDeniedException 则由 GlobalExceptionHandler 处理，两者输出结构一致。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        JsonAuthWriter.write(response, objectMapper, HttpStatus.FORBIDDEN.value(),
                ResultCode.FORBIDDEN, "无权限访问该接口");
    }
}
