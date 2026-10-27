package com.pyh.trade.common;

/**
 * 业务异常。
 * <p>Service 层校验失败时抛出，由 GlobalExceptionHandler 统一转成 {@code Result} + 对应 HTTP 状态码。
 * 不打印堆栈（业务异常不是系统故障），只按 warn 级别记录关键信息。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 业务异常分支高频出现，省略堆栈以降低开销
        return this;
    }
}
