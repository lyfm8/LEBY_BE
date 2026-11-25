package com.example.be.service.auth.implement;

import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.security.CookieProperties;
import com.example.be.security.JwtTokenProvider;
import com.example.be.dto.request.auth.LoginRequest;
import com.example.be.dto.request.auth.RegisterRequest;
import com.example.be.dto.request.auth.ResetPasswordRequest;
import com.example.be.dto.request.auth.SendOtpRequest;
import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.mapper.auth.AuthMapper;
import com.example.be.enums.auth.OtpPurpose;
import com.example.be.enums.user.ERole;
import com.example.be.entity.user.User;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.auth.service.AuthService;
import com.example.be.service.auth.service.MailService;
import com.example.be.service.auth.service.OtpService;
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
import java.util.Locale;

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
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProperties cookieProperties;
    private final OtpService otpService;
    private final MailService mailService;
    private final AuthMapper authMapper;

    // -----------------------------------------------------------------------
    // Send OTP
    // -----------------------------------------------------------------------

    /**
     * Gửi mã OTP 6 số qua email.
     * Không tạo tài khoản ở bước này.
     *
     * @param request chứa email và purpose ("REGISTER" hoặc "FORGOT_PASSWORD").
     */
    @Override
    public void sendOtp(SendOtpRequest request) {
        OtpPurpose purpose = parsePurpose(request.getPurpose());
        String email = normalizeEmail(request.getEmail());

        // Bước 1: Kiểm tra tồn tại email phù hợp với từng mục đích OTP
        if (purpose == OtpPurpose.REGISTER && userRepository.existsByEmail(email)) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được đăng ký trong hệ thống");
        }

        if (purpose == OtpPurpose.FORGOT_PASSWORD) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản với email này"));
            // Chặn đặt lại mật khẩu nếu tài khoản đang bị quản trị viên khóa
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new BaseException(ErrorCode.USER_IS_LOCKED, "Tài khoản này đang bị khóa, vui lòng liên hệ quản trị viên");
            }
        }

        // Bước 2: Sinh mã OTP 6 số (TTL 10 phút) lưu Cache và gửi email
        String otp = otpService.generateAndStore(email, purpose);
        mailService.sendOtpEmail(email, otp, purpose == OtpPurpose.REGISTER);

        log.info("[AUTH] OTP sent to {} for purpose {}", email, purpose);
    }

    // -----------------------------------------------------------------------
    // Register (UC01)
    // -----------------------------------------------------------------------

    /**
     * Đăng ký tài khoản học viên mới (UC01).
     * Bắt buộc OTP hợp lệ (BR01) trước khi khởi tạo User và cấp phiên đăng nhập JWT Cookie.
     *
     * @param request  thông tin đăng ký + mã OTP.
     * @param response HTTP response để gắn Cookie.
     * @return AuthResponse chứa thông tin user.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = normalizeEmail(request.getEmail());

        // Bước 1: Đối chiếu mật khẩu và xác nhận mật khẩu
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        // Bước 2: Xác minh mã OTP 6 chữ số (sai/hết hạn 10 phút/sai quá 5 lần sẽ ném BaseException)
        otpService.verify(email, OtpPurpose.REGISTER, request.getOtp().trim());

        // Bước 3: Kiểm tra lại email chưa tồn tại ngay trước khi insert (chống race condition)
        if (userRepository.existsByEmail(email)) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email này đã được đăng ký trong hệ thống");
        }

        // Bước 4: Tạo tài khoản mới với vai trò mặc định STUDENT (chống leo thang quyền từ client)
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(ERole.STUDENT);
        user.setIsActive(true);
        user.setTokenVersion(0);

        User savedUser = userRepository.save(user);
        log.info("[AUTH] New user registered: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // Bước 5: Cấp cặp Access/Refresh Token dạng HTTP-Only Cookie để học viên vào thẳng luồng chọn AIM
        issueTokenCookies(savedUser, response);

        return authMapper.toResponse(savedUser);
    }

    // -----------------------------------------------------------------------
    // Login (UC02)
    // -----------------------------------------------------------------------

    /**
     * Đăng nhập bằng Email / Mật khẩu (UC02).
     *
     * @param request  email và password.
     * @param response HTTP response để gắn Cookie.
     * @return AuthResponse chứa thông tin user kèm role (ADMIN/TEACHER/STUDENT) để FE phân luồng.
     */
    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String email = normalizeEmail(request.getEmail());

        // SECURITY: Trả chung một lỗi INVALID_CREDENTIALS cho cả sai email lẫn sai mật khẩu để chống dò tài khoản (Account Enumeration)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }

        // Kiểm tra trạng thái khóa tài khoản trước khi cấp JWT
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BaseException(ErrorCode.USER_IS_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        log.info("[AUTH] User logged in: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        issueTokenCookies(user, response);
        return authMapper.toResponse(user);
    }

    // -----------------------------------------------------------------------
    // Forgot / Reset Password (UC04)
    // -----------------------------------------------------------------------

    /**
     * Đặt lại mật khẩu mới sau khi xác minh OTP mục đích FORGOT_PASSWORD (UC04).
     * Tự động tăng tokenVersion nhằm thu hồi toàn bộ phiên đăng nhập cũ trên các thiết bị khác.
     *
     * @param request email, mã OTP 6 số, mật khẩu mới và xác nhận mật khẩu mới
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        // Bước 1: Kiểm tra khớp mật khẩu mới
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        // Bước 2: Tìm tài khoản và đảm bảo tài khoản chưa bị khóa
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BaseException(ErrorCode.USER_IS_LOCKED, "Tài khoản này đang bị khóa, không thể đặt lại mật khẩu");
        }

        // Bước 3: Xác minh mã OTP FORGOT_PASSWORD (hiệu lực 10 phút, dùng tối đa 1 lần, tối đa 5 lần sai)
        otpService.verify(email, OtpPurpose.FORGOT_PASSWORD, request.getOtp().trim());

        // Bước 4: Mã hóa BCrypt mật khẩu mới và tăng tokenVersion để vô hiệu hóa mọi JWT cũ đang còn hạn
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        int nextVersion = user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1;
        user.setTokenVersion(nextVersion);
        userRepository.save(user);

        log.info("[AUTH] Password reset successfully for userId={}, email={}", user.getId(), email);
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
    @Override
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

        // Chặn gia hạn token nếu tài khoản đã bị quản trị viên khóa sau thời điểm đăng nhập
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BaseException(ErrorCode.USER_IS_LOCKED, "Tài khoản đã bị khóa");
        }

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
    @Override
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
     * Chuẩn hóa địa chỉ email (cắt khoảng trắng thừa và viết thường) để tránh lỗi lệch hoa/thường khi đăng nhập.
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

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
