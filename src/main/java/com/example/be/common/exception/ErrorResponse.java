package com.example.be.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO thống nhất cho toàn bộ error response trong hệ thống.
 *
 * Format cơ bản:
 * {
 *     "timestamp": "2025-01-01T10:00:00",
 *     "status": 404,
 *     "error": "NOT_FOUND",
 *     "code": "RESOURCE_NOT_FOUND",
 *     "message": "Resource not found",
 *     "path": "/api/users/10"
 * }
 *
 * Khi có validation errors (field-level), thêm:
 * {
 *     ...,
 *     "errors": {
 *         "email": "Email must be valid",
 *         "username": "Username must not be blank"
 *     }
 * }
 *
 * Field "errors" chỉ xuất hiện khi có giá trị (JsonInclude.NON_NULL).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;

    /** HTTP status name. Ví dụ: "NOT_FOUND", "BAD_REQUEST". */
    private final String error;

    /** ErrorCode định danh lỗi. Ví dụ: "RESOURCE_NOT_FOUND", "VALIDATION_FAILED". */
    private final String code;

    private final String message;
    private final String path;

    /**
     * Chỉ có mặt khi xảy ra MethodArgumentNotValidException
     * hoặc ConstraintViolationException.
     * Key = tên field, Value = thông báo lỗi.
     */
    private final Map<String, String> errors;
}
