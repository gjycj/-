package com.house.deed.pavilion.common.exception;

import lombok.Getter;

/**
 * 业务异常（用于处理可预知的业务错误）
 */
@Getter
public class BusinessException extends RuntimeException {
    private int code; // 错误码
    private String message; // 错误信息

    public BusinessException(String message) {
        super(message);
        this.code = 1; // 默认业务错误码
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}