package com.example.be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception cho toàn bộ hệ thống.
 * Tất cả custom exception phải kế thừa class này.
 * GlobalExceptionHandler sẽ catch BaseException và xử lý thống nhất.
 */
@Getter
public class BaseException extends RuntimeException {

    private final HttpStatus status;

    public BaseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
