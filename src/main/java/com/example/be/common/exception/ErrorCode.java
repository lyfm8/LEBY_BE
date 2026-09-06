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

    /** HTTP 400 - Mã OTP không tồn tại trong Cache (chưa gửi hoặc đã hết hạn). */
    OTP_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "OTP_NOT_FOUND",
            "OTP not found or has not been sent"
    ),

    /** HTTP 400 - Mã OTP đã hết hạn (quá 10 phút). */
    OTP_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "OTP_EXPIRED",
            "OTP has expired"
    ),

    /** HTTP 400 - Mã OTP không chính xác. */
    OTP_INVALID(
            HttpStatus.BAD_REQUEST,
            "OTP_INVALID",
            "OTP is incorrect"
    ),

    /** HTTP 401 - Chưa xác thực, token không hợp lệ hoặc đã hết hạn. */
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Authentication required"
    ),

    /** HTTP 401 - Sai tên đăng nhập hoặc mật khẩu khi đăng nhập. */
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "Invalid username or password"
    ),

    /** HTTP 401 - Access Token hoặc Refresh Token đã hết hạn. */
    TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_EXPIRED",
            "Token has expired"
    ),

    /** HTTP 401 - Token bị giả mạo hoặc sai định dạng. */
    TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_INVALID",
            "Token is invalid"
    ),

    /**
     * HTTP 401 - Token cũ sau khi user logout hoặc đổi mật khẩu.
     * tokenVersion trong token không khớp với tokenVersion hiện tại trong DB.
     */
    TOKEN_VERSION_MISMATCH(
            HttpStatus.UNAUTHORIZED,
            "TOKEN_VERSION_MISMATCH",
            "Session has been invalidated, please login again"
    ),

    /** HTTP 401 - Gọi /refresh nhưng không có Refresh Token Cookie. */
    REFRESH_TOKEN_MISSING(
            HttpStatus.UNAUTHORIZED,
            "REFRESH_TOKEN_MISSING",
            "Refresh token is missing"
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
