package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 400 - Bad Request.
 * Dùng cho request không hợp lệ về mặt dữ liệu đầu vào,
 * nhưng không liên quan đến validation annotation (đã có MethodArgumentNotValidException).
 *
 * Ví dụ:
 * - Tham số không hợp lệ về mặt nghiệp vụ nhẹ.
 * - Format dữ liệu sai.
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
