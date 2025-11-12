package com.house.deed.pavilion.common.exception;

import com.house.deed.pavilion.common.dto.ResultDTO;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 处理租户相关异常
    @ExceptionHandler(TenantException.class)
    public ResultDTO<Void> handleTenantException(TenantException e) {
        log.warn("租户异常: {}", e.getMessage());
        int code = e.getCode() > 0 ? e.getCode() : 401;
        return ResultDTO.fail(code, e.getMessage());
    }

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ResultDTO<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResultDTO.fail(e.getCode(), e.getMessage());
    }

}