package com.example.be.service.dashboard.service;

import com.example.be.dto.response.dashboard.AdminDashboardSummaryResponse;

/**
 * Service định nghĩa các nghiệp vụ thống kê phân tích số liệu cho Admin Dashboard.
 */
public interface AdminDashboardService {

    /**
     * Lấy toàn bộ số liệu thống kê tổng hợp của hệ thống theo bộ lọc thời gian.
     *
     * @param timeRange Khoảng thời gian lọc: "all", "this_month", "30d", "7d"
     * @return AdminDashboardSummaryResponse DTO chứa 4 KPI cards, biểu đồ tăng trưởng, phân bố AIM, top mô-đun khó và nhật ký hoạt động
     */
    AdminDashboardSummaryResponse getDashboardSummary(String timeRange);
}
