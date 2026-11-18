package com.example.be.service.user.service;

import com.example.be.dto.request.user.ChangePasswordRequest;
import com.example.be.dto.request.user.UpdateProfileRequest;
import com.example.be.dto.response.user.ProfileResponse;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến hồ sơ cá nhân và ma trận năng lực của người dùng (UC15 & UC16).
 */
public interface ProfileService {

    /**
     * Lấy toàn bộ thông tin hồ sơ cá nhân, mục tiêu AIM và ma trận năng lực của người dùng hiện tại (UC15).
     *
     * @param userId ID định danh của người dùng (lấy từ JWT token context)
     * @return ProfileResponse DTO tổng hợp thông tin cá nhân, mục tiêu và danh sách kỹ năng
     */
    ProfileResponse getProfile(Long userId);

    /**
     * Cập nhật thông tin hồ sơ cá nhân: Họ tên, ngày sinh, avatar, kiểu người học (UC16).
     *
     * @param userId ID định danh của người dùng
     * @param request DTO chứa các thông tin cập nhật mới
     * @return ProfileResponse Thông tin hồ sơ mới nhất sau khi cập nhật thành công
     */
    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Đổi mật khẩu tài khoản người dùng và vô hiệu hóa các phiên đăng nhập cũ (UC16).
     *
     * @param userId ID định danh của người dùng
     * @param request DTO chứa mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
