package com.example.be.service.dashboard.service;

import com.example.be.dto.response.dashboard.UserDashboardSummaryResponse;

/**
 * Service Interface tổng hợp dữ liệu cho màn hình Dashboard học tập cá nhân của học viên (UC08).
 */
public interface UserDashboardService {

    /**
     * Tổng hợp toàn bộ số liệu, trạng thái vòng đời học tập, mô-đun đang học tiếp theo,
     * biểu đồ tiến độ tuần, đánh giá 7 Part và lịch sử hoạt động gần đây cho học viên.
     *
     * @param userId ID định danh của học viên đang đăng nhập (trích xuất từ SecurityContext)
     * @return {@link UserDashboardSummaryResponse} chứa đầy đủ dữ liệu hiển thị Dashboard
     */
    UserDashboardSummaryResponse getStudentDashboardSummary(Long userId);
}
