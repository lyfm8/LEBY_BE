package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 403 - Forbidden.
 * Dùng khi user đã xác thực (authenticated) nhưng không có quyền
 * thực hiện hành động trên resource.
 *
 * Ví dụ:
 * - Student cố tình truy cập API của Admin.
 * - User cố tình truy cập data của User khác.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
