package com.example.be.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Cấu hình Spring Security cho hệ thống.
 *
 * Chiến lược bảo mật:
 * - Stateless: không dùng Session, mọi request tự xác thực qua JWT Cookie.
 * - CSRF disabled: dựa hoàn toàn vào SameSite=Strict của Cookie để chống CSRF.
 *   Kẻ tấn công từ domain khác gửi request cross-site sẽ không được đính kèm Cookie.
 * - HTTP Basic và Form Login đều bị tắt.
 *
 * Phân quyền:
 * - Route-level: public endpoints được khai báo tại authorizeHttpRequests.
 * - Method-level: dùng @PreAuthorize(Roles.*) trên từng Controller method.
 *   @EnableMethodSecurity đã bật để hỗ trợ điều này.
 *
 * Khi role của user thay đổi, phải tăng tokenVersion để vô hiệu hóa
 * Access Token cũ còn chứa role cũ — tránh leo thang quyền.
 *
 * Luồng xác thực:
 *   Request → JwtAuthenticationFilter → SecurityContext → Controller
 *
 * Luồng lỗi:
 *   Thiếu/sai token     → 401 JSON (CustomAuthenticationEntryPoint)
 *   Đúng token, sai role → 403 JSON (CustomAccessDeniedHandler)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    /**
     * BCrypt PasswordEncoder với strength 10.
     * Dùng để hash password khi đăng ký và verify khi đăng nhập.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * CORS configuration.
     * withCredentials=true trên FE yêu cầu allowedOrigins phải là domain cụ thể,
     * không được dùng "*".
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",  // Vite dev server (default)
                "http://localhost:5174"  // Vite fallback port
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // Bắt buộc khi FE dùng withCredentials: true
        config.setMaxAge(3600L);          // Cache preflight 1 giờ

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS — phải đặt trước CSRF để preflight OPTIONS không bị chặn.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Tắt CSRF — dựa hoàn toàn vào SameSite=Strict của Cookie.
                .csrf(AbstractHttpConfigurer::disable)

                // Tắt Session — JWT là stateless.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Tắt HTTP Basic và Form Login mặc định.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // Authorization rules — route-level.
                // Phân quyền theo role được thực hiện ở method-level bằng @PreAuthorize(Roles.*).
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints — không cần token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/send-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // Tất cả endpoint còn lại yêu cầu xác thực.
                        // Quyền cụ thể (STUDENT/TEACHER/ADMIN) được kiểm tra bằng @PreAuthorize(Roles.*).
                        .anyRequest().authenticated()
                )

                // Custom error responses — trả JSON thay vì HTML mặc định của Spring Security.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)  // 401
                        .accessDeniedHandler(securityExceptionHandler)        // 403
                )

                // Thêm JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
