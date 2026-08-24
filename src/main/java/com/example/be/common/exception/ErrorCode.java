package com.example.be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Tập trung toàn bộ error code của hệ thống.
 *
 * Mỗi entry chứa:
 * - status: HTTP status trả về cho client.
 * - code:   Định danh lỗi duy nhất (dùng trong response JSON và logging).
 * - message: Thông báo mặc định. Có thể override khi throw BaseException.
 *
 * Cách dùng:
 * <pre>
 *     throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
 *     throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User with id " + id + " was not found");
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // -----------------------------------------------------------------------
    // 4xx - Client Errors
    // -----------------------------------------------------------------------

    /** HTTP 400 - Request không hợp lệ về mặt nghiệp vụ nhẹ. */
    BAD_REQUEST(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            "Bad request"
    ),

    /** HTTP 401 - Chưa xác thực, token không hợp lệ hoặc đã hết hạn. */
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Authentication required"
    ),

    /** HTTP 403 - Đã xác thực nhưng không có quyền thực hiện hành động. */
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "Access denied"
    ),

    /** HTTP 404 - Resource không tồn tại trong hệ thống. */
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "Resource not found"
    ),

    /** HTTP 409 - Email đã tồn tại trong hệ thống. */
    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "EMAIL_ALREADY_EXISTS",
            "Email already exists"
    ),

    /** HTTP 409 - Username đã được sử dụng. */
    USERNAME_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "USERNAME_ALREADY_EXISTS",
            "Username already exists"
    ),

    // -----------------------------------------------------------------------
    // 422 - Business Rule Violations (LEBY-specific)
    // -----------------------------------------------------------------------

    /**
     * HTTP 422 - Level/Module chưa được unlock mà user cố truy cập.
     */
    CONTENT_LOCKED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "CONTENT_LOCKED",
            "Content is not yet unlocked"
    ),

    /**
     * HTTP 422 - User đã hoàn thành test, không thể làm lại.
     */
    TEST_ALREADY_COMPLETED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "TEST_ALREADY_COMPLETED",
            "Test has already been completed"
    ),

    /**
     * HTTP 422 - Chưa hoàn thành điều kiện tiên quyết (lesson, level).
     */
    PREREQUISITE_NOT_MET(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PREREQUISITE_NOT_MET",
            "Prerequisites have not been met"
    ),

    /**
     * HTTP 422 - DiagnosticTest chưa được hoàn thành mà cố tạo LearningPath.
     */
    DIAGNOSTIC_NOT_COMPLETED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "DIAGNOSTIC_NOT_COMPLETED",
            "Diagnostic test must be completed first"
    );

    // -----------------------------------------------------------------------

    private final HttpStatus status;
    private final String code;
    private final String message;
}
