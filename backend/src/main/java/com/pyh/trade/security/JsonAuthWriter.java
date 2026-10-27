package com.pyh.trade.security;

import tools.jackson.databind.ObjectMapper;
import com.pyh.trade.common.Result;
import com.pyh.trade.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证/鉴权失败时的 JSON 写出工具。
 * <p>Spring Security 的 EntryPoint / AccessDeniedHandler / JWT 过滤器都发生在 Controller 之前，
 * 拿不到 @RestControllerAdvice 的处理能力，所以在这里统一写 {@code Result} JSON，
 * 保证前端 401/403 拿到的也是 JSON 而不是空 body 或 HTML。
 */
final class JsonAuthWriter {

    private JsonAuthWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, int httpStatus,
                      ResultCode resultCode, String msg) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(resultCode.getCode(), msg == null ? resultCode.getMsg() : msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
