package com.example.be.common.security;

import com.example.be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi 403 Forbidden theo chuẩn JSON của API.
 *
 * Được gọi khi user đã xác thực (có token hợp lệ) nhưng không đủ quyền
 * để thực hiện hành động — ví dụ: STUDENT gọi endpoint chỉ dành cho ADMIN.
 *
 * Thường xảy ra sau khi @PreAuthorize hoặc @Secured từ chối truy cập.
 *
 * Nếu không cấu hình AccessDeniedHandler này, Spring Security mặc định trả về
 * trang HTML 403 — không phù hợp với REST API.
 *
 * Response mẫu:
 * <pre>
 * {
 *   "status": 403,
 *   "error": "FORBIDDEN",
 *   "code": "FORBIDDEN",
 *   "message": "Access denied"
 * }
 * </pre>
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        log.warn("[SECURITY] 403 Forbidden — {}: {}",
                request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"code\":\"%s\",\"message\":\"%s\"}",
                errorCode.getStatus().value(),
                errorCode.getStatus().name(),
                errorCode.getCode(),
                errorCode.getMessage()
        ));
    }
}
