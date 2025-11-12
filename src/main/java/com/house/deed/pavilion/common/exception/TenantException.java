package com.house.deed.pavilion.common.exception;

/**
 * 租户相关异常（如租户不存在、租户ID为空等）
 */
public class TenantException extends RuntimeException {
    // 错误码（可选，用于前端区分具体错误类型）
    private int code;

    public TenantException(String message) {
        super(message);
    }

    public TenantException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}