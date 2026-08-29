package com.example.be.common.security;

import com.example.be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý lỗi 401 Unauthorized theo chuẩn JSON của API.
 *
 * Được gọi khi request chạm vào endpoint yêu cầu xác thực
 * nhưng không có hoặc không có token hợp lệ trong SecurityContext
 * (ví dụ: thiếu Cookie, token hết hạn đã bị JwtAuthenticationFilter xử lý
 * nhưng SecurityContext không được set).
 *
 * Nếu không cấu hình EntryPoint này, Spring Security mặc định trả về
 * trang HTML 401 — không phù hợp với REST API.
 *
 * Response mẫu:
 * <pre>
 * {
 *   "status": 401,
 *   "error": "UNAUTHORIZED",
 *   "code": "UNAUTHORIZED",
 *   "message": "Authentication required"
 * }
 * </pre>
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        log.warn("[SECURITY] 401 Unauthorized — {}: {}",
                request.getRequestURI(), authException.getMessage());

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
