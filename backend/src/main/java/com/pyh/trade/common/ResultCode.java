package com.pyh.trade.common;

/**
 * 业务状态码字典（与 HTTP 状态码对齐，便于前端统一处理）。
 * <p>被 {@link Result}、{@link BusinessException} 与 GlobalExceptionHandler 使用。
 */
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "success"),
    /** 参数校验失败 / 业务校验失败 */
    PARAM_ERROR(400, "参数校验失败"),
    /** 未登录或登录已失效 */
    UNAUTHORIZED(401, "未登录或登录已失效"),
    /** 无权限 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 重复提交 / 并发冲突（状态机 CAS 失败） */
    CONFLICT(409, "操作冲突，请刷新后重试"),
    /** 限流 */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后重试"),
    /** 系统异常 */
    SERVER_ERROR(500, "系统异常，请稍后重试"),
    /** 依赖的中间件不可用（Redis 故障时认证链路降级提示） */
    SERVICE_UNAVAILABLE(503, "认证服务暂不可用，请稍后重试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
