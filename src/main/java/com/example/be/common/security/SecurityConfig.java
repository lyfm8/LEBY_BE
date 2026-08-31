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

/**
 * Cấu hình Spring Security cho hệ thống.
 *
 * Chiến lược bảo mật:
 * - Stateless: không dùng Session, mọi request tự xác thực qua JWT Cookie.
 * - CSRF disabled: dựa hoàn toàn vào SameSite=Strict của Cookie để chống CSRF.
 *   Kẻ tấn công từ domain khác gửi request cross-site sẽ không được đính kèm Cookie.
 * - HTTP Basic và Form Login đều bị tắt.
 *
 * Luồng xác thực:
 *   Request → JwtAuthenticationFilter → SecurityContext → Controller
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * BCrypt PasswordEncoder với strength 10.
     * Dùng để hash password khi đăng ký và verify khi đăng nhập.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF — dựa hoàn toàn vào SameSite=Strict của Cookie.
                .csrf(AbstractHttpConfigurer::disable)

                // Tắt Session — JWT là stateless.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Tắt HTTP Basic và Form Login mặc định.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // Authorization rules.
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints — không cần token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/send-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // Tất cả endpoint còn lại yêu cầu xác thực.
                        .anyRequest().authenticated()
                )

                // Thêm JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
