package com.example.be.common.security;

import com.example.be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý tập trung các lỗi bảo mật của Spring Security — trả về JSON thay vì HTML mặc định.
 *
 * Implements cả hai interface để dùng chung một bean trong SecurityConfig:
 *
 *   AuthenticationEntryPoint → 401 Unauthorized
 *     Được gọi khi request thiếu token hoặc token không hợp lệ.
 *
 *   AccessDeniedHandler → 403 Forbidden
 *     Được gọi khi user đã xác thực nhưng không đủ role.
 *
 * Wire trong SecurityConfig:
 * <pre>
 *   .exceptionHandling(ex -> ex
 *       .authenticationEntryPoint(securityExceptionHandler)
 *       .accessDeniedHandler(securityExceptionHandler)
 *   )
 * </pre>
 */
@Slf4j
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // -----------------------------------------------------------------------
    // 401 — Chưa xác thực
    // -----------------------------------------------------------------------

    /**
     * Được gọi khi request chạm vào endpoint yêu cầu đăng nhập
     * nhưng không có hoặc token không hợp lệ trong SecurityContext.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("[SECURITY] 401 Unauthorized — {}: {}",
                request.getRequestURI(), authException.getMessage());
        writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }

    // -----------------------------------------------------------------------
    // 403 — Đã xác thực nhưng sai role
    // -----------------------------------------------------------------------

    /**
     * Được gọi khi user đã đăng nhập (token hợp lệ) nhưng không đủ role
     * để thực hiện hành động — thường do @PreAuthorize từ chối.
     *
     * Lưu ý: Khi ADMIN thay đổi role của user, phải tăng tokenVersion
     * để vô hiệu hóa Access Token cũ còn chứa role cũ.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("[SECURITY] 403 Forbidden — {}: {}",
                request.getRequestURI(), accessDeniedException.getMessage());
        writeErrorResponse(response, ErrorCode.FORBIDDEN);
    }

    // -----------------------------------------------------------------------
    // Shared
    // -----------------------------------------------------------------------

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
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
