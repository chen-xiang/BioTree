/**
 * 全局异常处理，统一输出 ApiResponse。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = mapStatus(code);
        return ResponseEntity.status(status).body(ApiResponse.fail(code, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        // 不把内部校验细节直接回传；统一文案
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    private static HttpStatus mapStatus(ErrorCode code) {
        int value = code.getCode();
        if (value >= 40000 && value < 40100) {
            return HttpStatus.BAD_REQUEST;
        }
        if (value >= 40100 && value < 40300) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (value >= 40300 && value < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (value >= 40400 && value < 40900) {
            return HttpStatus.NOT_FOUND;
        }
        if (value >= 40900 && value < 50000) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
