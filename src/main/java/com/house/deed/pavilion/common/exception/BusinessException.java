package com.house.deed.pavilion.common.exception;

import lombok.Getter;

/**
 * 业务异常（用于处理可预知的业务错误）
 */
@Getter
public class BusinessException extends RuntimeException {
    private int code; // 错误码

    // 默认错误码（1-业务错误）
    public BusinessException(String message) {
        super(message);
        this.code = 1;
    }

    // 自定义错误码（如404-资源不存在、400-参数错误）
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}