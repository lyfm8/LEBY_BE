package com.example.be.service.user.service;

import com.example.be.dto.request.user.UpdateUserRoleRequest;
import com.example.be.dto.response.user.AdminUserDetailResponse;
import com.example.be.dto.response.user.UserActiveExamStatusResponse;
import com.example.be.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface nghiệp vụ Quản lý tài khoản người dùng cho Quản trị viên (UC A02).
 */
public interface UserService {

    /**
     * Lấy danh sách tài khoản người dùng có phân trang và lọc theo từ khóa, vai trò, trạng thái và mốc AIM.
     */
    Page<UserResponse> getAllUsers(String keyword, String role, Boolean isActive, Integer aimScore, Pageable pageable);

    /**
     * Xem chi tiết thông tin tài khoản và toàn bộ lịch sử chẩn đoán, lộ trình học, thi vượt ải, thi thử của học viên.
     */
    AdminUserDetailResponse getUserDetail(Long id);

    /**
     * Kiểm tra tài khoản học viên có đang trong phòng thi trực tuyến nào không (UC A02 - Ngoại lệ 7d).
     */
    UserActiveExamStatusResponse getActiveExamStatus(Long id);

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (tuân thủ BR11 và ngoại lệ 7d).
     */
    UserResponse toggleUserStatus(Long id, boolean force);

    /**
     * Nâng quyền hoặc hạ quyền giữa STUDENT và TEACHER (UC A02 - Luồng 7c).
     */
    UserResponse updateUserRole(Long id, UpdateUserRoleRequest request);

    /**
     * Sinh mật khẩu ngẫu nhiên đạt chuẩn đăng ký, lưu DB và gửi email cho người dùng trong cùng transaction.
     */
    void resetUserPassword(Long id);

    /**
     * Xuất toàn bộ danh sách người dùng khớp bộ lọc hiện tại ra tệp Excel (.xlsx).
     */
    byte[] exportUsersToExcel(String keyword, String role, Boolean isActive, Integer aimScore);
}
