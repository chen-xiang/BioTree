/**
 * 业务错误码定义。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.api.common;

public enum ErrorCode {

    SUCCESS(0, "OK"),
    BAD_REQUEST(40000, "Bad request"),
    UNAUTHORIZED(40100, "Unauthorized"),
    FORBIDDEN(40300, "Forbidden"),
    NOT_FOUND(40400, "Not found"),
    CONFLICT(40900, "Conflict"),
    INTERNAL_ERROR(50000, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
