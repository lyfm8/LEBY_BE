package com.example.be.features.auth.controller;

import com.example.be.common.dto.ApiResponse;
import com.example.be.features.auth.dto.request.LoginRequest;
import com.example.be.features.auth.dto.request.RegisterRequest;
import com.example.be.features.auth.dto.request.SendOtpRequest;
import com.example.be.features.auth.dto.response.AuthResponse;
import com.example.be.features.auth.service.AuthService;
import com.example.be.features.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API Authentication.
 *
 * Tất cả JWT được gắn vào HTTP-Only Cookie trong Response Header (Set-Cookie).
 * Client (Browser) sẽ tự động gửi Cookie theo mỗi request sau đó.
 *
 * Endpoints:
 *   POST /api/auth/register/send-otp  — Gửi OTP 6 số qua email (public)
 *   POST /api/auth/register           — Đăng ký (cần OTP hợp lệ) (public)
 *   POST /api/auth/login              — Đăng nhập (public)
 *   POST /api/auth/refresh            — Gia hạn Access Token (public, cần Refresh Cookie)
 *   POST /api/auth/logout             — Đăng xuất (yêu cầu xác thực)
 *   GET  /api/auth/me                 — Lấy thông tin user hiện tại (yêu cầu xác thực)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Gửi OTP 6 số đến email.
     * Purpose: "REGISTER" hoặc "FORGOT_PASSWORD".
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendOtp(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "OTP has been sent to " + request.getEmail())
        );
    }

    /**
     * Đăng ký tài khoản mới.
     * OTP phải được gửi và xác minh thành công trước khi tài khoản được tạo.
     * Trả về thông tin user + gắn accessToken + refreshToken vào Cookie.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(request, response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Registration successful", authResponse)
        );
    }

    /**
     * Đăng nhập bằng Username / Mật khẩu.
     * Trả về thông tin user + gắn accessToken + refreshToken vào Cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login successful", authResponse)
        );
    }

    /**
     * Gia hạn Access Token sử dụng Refresh Token trong Cookie.
     * Không cần body. Refresh Token được đọc tự động từ Cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.refresh(request, response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Access token refreshed")
        );
    }

    /**
     * Đăng xuất.
     * Tăng tokenVersion trong DB → vô hiệu hóa toàn bộ token cũ.
     * Xóa cả accessToken và refreshToken Cookie.
     * Yêu cầu xác thực (phải có accessToken Cookie hợp lệ).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser,
            HttpServletResponse response
    ) {
        authService.logout(currentUser.getId(), response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Logged out successfully")
        );
    }

    /**
     * Lấy thông tin user đang đăng nhập.
     * @AuthenticationPrincipal lấy User object từ SecurityContext
     * (đã được set bởi JwtAuthenticationFilter).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Success", new AuthResponse(currentUser))
        );
    }
}
