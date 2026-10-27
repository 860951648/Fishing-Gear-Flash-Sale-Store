package com.pyh.trade.security;

import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证入口处理器。
 * <p>请求没有有效 token 却访问受保护接口时由 Spring Security 调用，
 * 返回 401 + {@code Result} JSON（Spring Security 7 默认行为是返回空 body，前端无法提示）。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        JsonAuthWriter.write(response, objectMapper, HttpStatus.UNAUTHORIZED.value(),
                ResultCode.UNAUTHORIZED, "未登录或登录已失效");
    }
}
