package com.example.be.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý tập trung tất cả exception trong hệ thống.
 *
 * Thứ tự ưu tiên xử lý:
 * 1. BaseException (application / business exception duy nhất)
 * 2. Spring MVC validation exceptions
 * 3. Spring Data / JPA exceptions
 * 4. Generic fallback (Exception)
 *
 * Không expose stack trace, SQL, hoặc thông tin nhạy cảm cho client.
 *
 * Kiến trúc exception:
 * <pre>
 *   BaseException              → application / business exception
 *   Spring Validation Exception → GlobalExceptionHandler
 *   Spring MVC Exception        → GlobalExceptionHandler
 *   Database Exception          → GlobalExceptionHandler
 *   Unknown Exception           → Generic Exception Handler (500)
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 1. BaseException — Application / Business Exception
    // -----------------------------------------------------------------------

    /**
     * Xử lý tất cả BaseException được throw trong application.
     * Lấy HTTP status, error code và message từ {@link ErrorCode}.
     *
     * Response format:
     * {
     *   "timestamp": "...",
     *   "status": 404,
     *   "error": "NOT_FOUND",
     *   "code": "RESOURCE_NOT_FOUND",
     *   "message": "User with id 1 was not found",
     *   "path": "/api/users/1"
     * }
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {

        log.warn(
                "[{}] {} - {}",
                ex.getStatus().value(),
                request.getRequestURI(),
                ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().name())
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // -----------------------------------------------------------------------
    // 2. Spring MVC Validation Exceptions
    // -----------------------------------------------------------------------

    /**
     * Xử lý @Valid / @Validated thất bại trên @RequestBody.
     * Trả về danh sách lỗi theo từng field.
     *
     * Yêu cầu dependency: spring-boot-starter-validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("[400] {} - Validation failed: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .code("VALIDATION_FAILED")
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Xử lý @Validated thất bại trên @RequestParam / @PathVariable.
     * Yêu cầu dependency: spring-boot-starter-validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            // Lấy tên field ngắn gọn (bỏ tên method prefix)
            if (field.contains(".")) {
                field = field.substring(field.lastIndexOf('.') + 1);
            }
            fieldErrors.put(field, violation.getMessage());
        }

        log.warn("[400] {} - Constraint violation: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .code("VALIDATION_FAILED")
                .message("Validation failed")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Xử lý type mismatch trên @PathVariable / @RequestParam.
     * Ví dụ: truyền "abc" cho param kiểu Long.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format(
                "Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("[400] {} - Type mismatch: {}", request.getRequestURI(), message);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .code("TYPE_MISMATCH")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Xử lý request body không đọc được (JSON malformed, sai kiểu dữ liệu).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("[400] {} - Message not readable: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .code("MESSAGE_NOT_READABLE")
                .message("Request body is malformed or cannot be read")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    // -----------------------------------------------------------------------
    // 3. Database / JPA Exceptions
    // -----------------------------------------------------------------------

    /**
     * Xử lý vi phạm unique constraint hoặc foreign key ở tầng DB.
     * Không expose thông tin DB (table name, column name, SQL query).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("[409] {} - Data integrity violation: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.name())
                .code("DATA_INTEGRITY_VIOLATION")
                .message("The request conflicts with the current state of the resource")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // -----------------------------------------------------------------------
    // 4. Fallback - Generic Exception
    // -----------------------------------------------------------------------

    /**
     * Fallback cho tất cả exception không được xử lý ở trên.
     * Trả về 500 Internal Server Error.
     * Không expose stack trace hoặc internal error message cho client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("[500] {} - Unexpected error: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.internalServerError().body(body);
    }
}
