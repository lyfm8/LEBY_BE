package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 401 - Unauthorized.
 * Dùng cho request chưa được xác thực hoặc authentication thất bại.
 *
 * Ví dụ:
 * - Token không hợp lệ hoặc đã hết hạn.
 * - Sai username/password khi đăng nhập.
 * - Chưa đăng nhập mà truy cập resource cần auth.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
