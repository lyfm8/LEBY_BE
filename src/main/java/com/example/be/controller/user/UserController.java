package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.ChangePasswordRequest;
import com.example.be.dto.request.user.UpdateProfileRequest;
import com.example.be.dto.response.user.ProfileResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý các yêu cầu liên quan đến hồ sơ cá nhân và bảo mật tài khoản người dùng (UC15 & UC16).
 *
 * Endpoints:
 *   GET  /api/users/profile         — Xem thông tin hồ sơ và ma trận năng lực
 *   PUT  /api/users/profile         — Cập nhật thông tin cá nhân (họ tên, ngày sinh, avatar, kiểu người học)
 *   POST /api/users/change-password — Đổi mật khẩu tài khoản
 */
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profileService;

    /**
     * Lấy thông tin hồ sơ cá nhân, mục tiêu AIM và ma trận năng lực của người dùng đang đăng nhập (UC15).
     *
     * @param currentUser Thực thể User được tiêm tự động từ Spring Security Context
     * @return ResponseEntity bọc trong ApiResponse chứa dữ liệu ProfileResponse
     */
    @GetMapping("/profile")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        ProfileResponse profile = profileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<ProfileResponse>builder()
                .success(true)
                .message("Tải thông tin hồ sơ cá nhân thành công")
                .data(profile)
                .build());
    }

    /**
     * Cập nhật thông tin cá nhân của người dùng (UC16).
     *
     * @param currentUser Thực thể User được tiêm từ Security Context (chống tấn công IDOR)
     * @param request DTO chứa các thông tin cập nhật hợp lệ
     * @return ResponseEntity bọc trong ApiResponse chứa hồ sơ cập nhật mới nhất
     */
    @PutMapping("/profile")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse updatedProfile = profileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<ProfileResponse>builder()
                .success(true)
                .message("Cập nhật thông tin cá nhân thành công")
                .data(updatedProfile)
                .build());
    }

    /**
     * Đổi mật khẩu tài khoản và vô hiệu hóa các token cũ (UC16).
     *
     * @param currentUser Thực thể User được tiêm từ Security Context
     * @param request DTO chứa mật khẩu hiện tại và mật khẩu mới
     * @return ResponseEntity bọc trong ApiResponse thông báo thành công
     */
    @PostMapping("/change-password")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đổi mật khẩu tài khoản thành công")
                .build());
    }
}
