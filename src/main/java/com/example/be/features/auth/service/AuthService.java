package com.example.be.features.auth.service;

import com.example.be.common.exception.BaseException;
import com.example.be.common.exception.ErrorCode;
import com.example.be.common.security.CookieProperties;
import com.example.be.common.security.JwtTokenProvider;
import com.example.be.features.auth.dto.request.LoginRequest;
import com.example.be.features.auth.dto.request.RegisterRequest;
import com.example.be.features.auth.dto.request.SendOtpRequest;
import com.example.be.features.auth.dto.response.AuthResponse;
import com.example.be.features.auth.enums.OtpPurpose;
import com.example.be.features.user.entity.User;
import com.example.be.features.user.enums.ERole;
import com.example.be.features.user.repo.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Service xử lý toàn bộ nghiệp vụ Authentication.
 *
 * Luồng đăng ký:
 *   sendOtp() → register(otp) → [OTP hợp lệ] → lưu User → cấp JWT Cookie
 *
 * Luồng đăng nhập:
 *   login() → [credential đúng] → cấp JWT Cookie
 *
 * Luồng đăng xuất:
 *   logout() → tăng tokenVersion trong DB → xóa Cookie
 *
 * Luồng refresh:
 *   refresh() → verify refreshToken + tokenVersion → cấp accessToken mới
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProperties cookieProperties;
    private final OtpService otpService;
    private final MailService mailService;

    // -----------------------------------------------------------------------
    // Send OTP
    // -----------------------------------------------------------------------

    /**
     * Gửi mã OTP 6 số qua email.
     * Không tạo tài khoản ở bước này.
     *
     * @param request chứa email và purpose ("REGISTER" hoặc "FORGOT_PASSWORD").
     */
    public void sendOtp(SendOtpRequest request) {
        OtpPurpose purpose = parsePurpose(request.getPurpose());

        if (purpose == OtpPurpose.REGISTER && userRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (purpose == OtpPurpose.FORGOT_PASSWORD && !userRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "No account found with email: " + request.getEmail());
        }

        String otp = otpService.generateAndStore(request.getEmail(), purpose);
        mailService.sendOtpEmail(request.getEmail(), otp, purpose == OtpPurpose.REGISTER);

        log.info("[AUTH] OTP sent to {} for purpose {}", request.getEmail(), purpose);
    }

    // -----------------------------------------------------------------------
    // Register
    // -----------------------------------------------------------------------

    /**
     * Đăng ký tài khoản mới.
     * Bắt buộc OTP hợp lệ trước khi tạo User.
     *
     * @param request  thông tin đăng ký + mã OTP.
     * @param response HTTP response để gắn Cookie.
     * @return AuthResponse chứa thông tin user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        // 1. Validate password match.
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Passwords do not match");
        }

        // 2. Xác minh OTP — throw BaseException nếu sai/hết hạn/không tồn tại.
        otpService.verify(request.getEmail(), OtpPurpose.REGISTER, request.getOtp());

        // 3. Kiểm tra email và username chưa tồn tại.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BaseException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 4. Tạo và lưu User mới.
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(ERole.STUDENT);
        user.setIsActive(true);
        user.setTokenVersion(0);

        User savedUser = userRepository.save(user);
        log.info("[AUTH] New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // 5. Cấp JWT và gắn Cookie.
        issueTokenCookies(savedUser, response);

        return new AuthResponse(savedUser);
    }

    // -----------------------------------------------------------------------
    // Login
    // -----------------------------------------------------------------------

    /**
     * Đăng nhập bằng Email / Mật khẩu.
     *
     * @param request  email và password.
     * @param response HTTP response để gắn Cookie.
     * @return AuthResponse chứa thông tin user.
     */
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("[AUTH] User logged in: id={}, email={}", user.getId(), user.getEmail());

        issueTokenCookies(user, response);
        return new AuthResponse(user);
    }

    // -----------------------------------------------------------------------
    // Refresh
    // -----------------------------------------------------------------------

    /**
     * Gia hạn Access Token sử dụng Refresh Token trong Cookie.
     * Không yêu cầu user đăng nhập lại.
     *
     * @param request  HTTP request để đọc Refresh Token Cookie.
     * @param response HTTP response để gắn Access Token Cookie mới.
     */
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, cookieProperties.getRefreshTokenName());
        if (refreshToken == null) {
            throw new BaseException(ErrorCode.REFRESH_TOKEN_MISSING);
        }

        // Parse và validate Refresh Token.
        Claims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(claims);
        Integer tokenVersion = jwtTokenProvider.getVersion(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.TOKEN_INVALID));

        if (!user.getTokenVersion().equals(tokenVersion)) {
            throw new BaseException(ErrorCode.TOKEN_VERSION_MISMATCH);
        }

        // Chỉ cấp lại Access Token. Refresh Token vẫn giữ nguyên.
        // Role được đọc từ DB vì đây là thời điểm duy nhất cần đảm bảo role là mới nhất.
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getTokenVersion(), user.getRole().name());
        ResponseCookie accessCookie = jwtTokenProvider.buildAccessTokenCookie(newAccessToken);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        log.debug("[AUTH] Access token refreshed for userId={}", userId);
    }

    // -----------------------------------------------------------------------
    // Logout
    // -----------------------------------------------------------------------

    /**
     * Đăng xuất: tăng tokenVersion trong DB, xóa cả 2 Cookie.
     * Toàn bộ JWT cũ bị vô hiệu hóa ngay lập tức.
     *
     * @param userId   ID của user đang đăng nhập (lấy từ SecurityContext trong Controller).
     * @param response HTTP response để xóa Cookie.
     */
    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        userRepository.incrementTokenVersion(userId);

        response.addHeader(HttpHeaders.SET_COOKIE, jwtTokenProvider.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtTokenProvider.clearRefreshTokenCookie().toString());

        log.info("[AUTH] User logged out: id={}", userId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Sinh cả Access Token và Refresh Token, gắn vào Response Cookie.
     *
     * Role được nhung vào Access Token claim để JwtAuthenticationFilter
     * đọc trực tiếp mà không cần query DB cho phân quyền.
     */
    private void issueTokenCookies(User user, HttpServletResponse response) {
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getTokenVersion(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getTokenVersion());

        response.addHeader(HttpHeaders.SET_COOKIE, jwtTokenProvider.buildAccessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtTokenProvider.buildRefreshTokenCookie(refreshToken).toString());
    }

    /**
     * Đọc giá trị Cookie theo tên từ HttpServletRequest.
     */
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Parse chuỗi purpose thành OtpPurpose enum.
     * Trả về BAD_REQUEST nếu giá trị không hợp lệ.
     */
    private OtpPurpose parsePurpose(String purpose) {
        try {
            return OtpPurpose.valueOf(purpose.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Invalid OTP purpose: " + purpose);
        }
    }
}
