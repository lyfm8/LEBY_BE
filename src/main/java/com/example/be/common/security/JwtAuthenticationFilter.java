package com.example.be.common.security;

import com.example.be.common.exception.BaseException;
import com.example.be.common.exception.ErrorCode;
import com.example.be.features.user.entity.User;
import com.example.be.features.user.repo.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Filter xác thực JWT chạy một lần cho mỗi request.
 *
 * Luồng:
 * 1. Đọc accessToken từ Cookie.
 * 2. Nếu không có Cookie → bỏ qua (request public sẽ vẫn hoạt động).
 * 3. Có Cookie → parseAccessToken() → lấy userId và version.
 * 4. Truy vấn DB lấy User theo userId.
 * 5. So sánh tokenVersion: nếu không khớp → throw TOKEN_VERSION_MISMATCH.
 * 6. Tạo Authentication và set vào SecurityContextHolder.
 *
 * Lưu ý: Nếu token hết hạn hoặc bị giả mạo, JwtTokenProvider sẽ throw BaseException.
 * GlobalExceptionHandler KHÔNG hứng được exception trong Filter.
 * Thay vào đó, SecurityConfig phải cấu hình AuthenticationEntryPoint để xử lý.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CookieProperties cookieProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractTokenFromCookie(request, cookieProperties.getAccessTokenName());

        if (token == null) {
            // Không có Cookie → tiếp tục filter chain.
            // Request yêu cầu auth sẽ bị SecurityConfig từ chối sau.
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseAccessToken(token);

            Long userId = jwtTokenProvider.getUserId(claims);
            Integer tokenVersion = jwtTokenProvider.getVersion(claims);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseException(ErrorCode.TOKEN_INVALID));

            if (!user.getTokenVersion().equals(tokenVersion)) {
                throw new BaseException(ErrorCode.TOKEN_VERSION_MISMATCH);
            }

            // Tạo Authentication object với ROLE_ prefix theo chuẩn Spring Security.
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (BaseException ex) {
            // Ghi log và trả lỗi 401 trực tiếp từ Filter.
            // Không thể dùng GlobalExceptionHandler ở đây vì nằm ngoài DispatcherServlet.
            log.warn("[AUTH FILTER] {} - {}: {}", request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(ex.getStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(buildErrorJson(ex));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Đọc giá trị Cookie theo tên.
     *
     * @return token string nếu tìm thấy, null nếu không có.
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Tạo JSON error response đơn giản để ghi thẳng vào HttpServletResponse.
     * Không dùng ErrorResponse builder để tránh dependency phức tạp trong Filter.
     */
    private String buildErrorJson(BaseException ex) {
        return String.format(
                "{\"status\":%d,\"error\":\"%s\",\"code\":\"%s\",\"message\":\"%s\"}",
                ex.getStatus().value(),
                ex.getStatus().name(),
                ex.getErrorCode().getCode(),
                ex.getMessage()
        );
    }
}
