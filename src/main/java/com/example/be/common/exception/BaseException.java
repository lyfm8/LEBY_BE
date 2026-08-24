package com.example.be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception cho toàn bộ hệ thống.
 *
 * Là exception duy nhất cho application/business exception.
 * GlobalExceptionHandler sẽ catch BaseException và xử lý thống nhất.
 *
 * Cách dùng:
 * <pre>
 *     // Dùng default message từ ErrorCode
 *     throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
 *
 *     // Override message với context cụ thể
 *     throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User with id " + id + " was not found");
 * </pre>
 */
@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Throw với default message được định nghĩa trong ErrorCode.
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Throw với message tùy chỉnh cho context cụ thể.
     * ErrorCode vẫn xác định HTTP status và code trong response.
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Trả về HTTP status từ ErrorCode.
     */
    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
