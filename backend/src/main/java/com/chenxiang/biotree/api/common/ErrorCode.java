/**
 * 业务错误码定义。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 细粒度业务码供前端 i18n 映射
 */
package com.chenxiang.biotree.api.common;

public enum ErrorCode {

    SUCCESS(0, "OK"),
    BAD_REQUEST(40000, "Bad request"),
    INVALID_PARENT(40001, "Invalid parent"),
    INVALID_MOVE(40002, "Invalid move"),
    INVALID_QUERY(40003, "Invalid query"),
    INVALID_UPLOAD(40004, "Invalid upload"),
    UNAUTHORIZED(40100, "Unauthorized"),
    FORBIDDEN(40300, "Forbidden"),
    NOT_FOUND(40400, "Not found"),
    TAXON_NOT_FOUND(40401, "Taxon not found"),
    MEDIA_NOT_FOUND(40402, "Media not found"),
    CONFLICT(40900, "Conflict"),
    TAXON_HAS_CHILDREN(40901, "Cannot delete taxon with children"),
    DUPLICATE_NAME(40902, "Scientific name already exists under the same parent"),
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
