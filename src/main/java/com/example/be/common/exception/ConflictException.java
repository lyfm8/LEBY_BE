package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 409 - Conflict.
 * Dùng khi dữ liệu bị conflict hoặc vi phạm unique constraint theo nghiệp vụ.
 *
 * Ví dụ:
 * - Email đã tồn tại khi đăng ký.
 * - Username đã được sử dụng.
 * - Dữ liệu trùng lặp về mặt nghiệp vụ.
 *
 * Lưu ý: DataIntegrityViolationException từ DB cũng được map → 409
 * trong GlobalExceptionHandler, nhưng ConflictException dành cho
 * validation ở tầng Service trước khi insert DB.
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
