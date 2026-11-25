package com.example.be.controller.auth;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.auth.LoginRequest;
import com.example.be.dto.request.auth.RegisterRequest;
import com.example.be.dto.request.auth.ResetPasswordRequest;
import com.example.be.dto.request.auth.SendOtpRequest;
import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.entity.user.User;
import com.example.be.mapper.auth.AuthMapper;
import com.example.be.service.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý các API Authentication (UC01 Đăng ký, UC02 Đăng nhập, UC03 Đăng xuất, UC04 Quên mật khẩu).
 *
 * Tất cả JWT được gắn vào HTTP-Only Cookie trong Response Header (Set-Cookie).
 * Client (Browser) sẽ tự động gửi Cookie theo mỗi request sau đó.
 *
 * Endpoints:
 *   POST /api/auth/send-otp (và /register/send-otp) — Gửi OTP 6 số qua email (public)
 *   POST /api/auth/register                         — Đăng ký tài khoản mới cần OTP (public)
 *   POST /api/auth/login                            — Đăng nhập Email / Mật khẩu (public)
 *   POST /api/auth/reset-password                   — Đặt lại mật khẩu mới qua OTP (public)
 *   POST /api/auth/refresh                          — Gia hạn Access Token từ Refresh Cookie (public)
 *   POST /api/auth/logout                           — Đăng xuất, vô hiệu hóa token cũ (authenticated)
 *   GET  /api/auth/me                               — Lấy thông tin user hiện tại (authenticated)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    /**
     * Gửi OTP 6 số đến email (hỗ trợ cả đường dẫn chuẩn /send-otp và đường dẫn cũ /register/send-otp).
     * Purpose: "REGISTER" hoặc "FORGOT_PASSWORD".
     */
    @PostMapping({"/send-otp", "/register/send-otp"})
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendOtp(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mã OTP đã được gửi đến email " + request.getEmail())
        );
    }

    /**
     * Đăng ký tài khoản mới (UC01).
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
                new ApiResponse<>(true, "Đăng ký tài khoản thành công", authResponse)
        );
    }

    /**
     * Đăng nhập bằng Email / Mật khẩu (UC02).
     * Trả về thông tin user (kèm role) + gắn accessToken + refreshToken vào Cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Đăng nhập thành công", authResponse)
        );
    }

    /**
     * Đặt lại mật khẩu mới sau khi xác minh OTP mục đích FORGOT_PASSWORD (UC04).
     * Toàn bộ phiên đăng nhập cũ sẽ bị thu hồi qua cơ chế tăng tokenVersion.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.")
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
     * Đăng xuất cho tất cả vai trò (STUDENT, TEACHER, ADMIN) — UC03.
     * Tăng tokenVersion trong DB để vô hiệu hóa toàn bộ token cũ và xóa cả accessToken lẫn refreshToken Cookie.
     * Cho phép gọi ngay cả khi Access Token vừa hết hạn để đảm bảo không bị kẹt Cookie cũ.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        authService.logout(userId, request, response);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Đăng xuất thành công")
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
                new ApiResponse<>(true, "Success", authMapper.toResponse(currentUser))
        );
    }
}
