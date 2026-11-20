package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.be.security.Roles;
import com.example.be.service.dashboard.service.AdminDashboardService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản trị cung cấp API thống kê phân tích số liệu nền tảng LEBY
 * (Admin Dashboard A01).
 * Khóa chặt quyền truy cập duy nhất cho Quản trị viên (ADMIN).
 */
@Validated
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN)
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Lấy dữ liệu thống kê tổng hợp toàn diện cho Admin Dashboard.
     *
     * @param timeRange Khoảng thời gian lọc ("all", "this_month", "30d", "7d")
     * @return ApiResponse bọc AdminDashboardSummaryResponse
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getDashboardSummary(
            @RequestParam(defaultValue = "all") @Pattern(regexp = "^(?i)(all|this_month|30d|7d)$", message = "Khoảng thời gian lọc không hợp lệ. Chỉ chấp nhận: all, this_month, 30d, 7d") String timeRange) {
        AdminDashboardSummaryResponse data = adminDashboardService.getDashboardSummary(timeRange);
        return ResponseEntity.ok(ApiResponse.<AdminDashboardSummaryResponse>builder()
                .success(true)
                .message("Lấy dữ liệu thống kê tổng quan thành công")
                .data(data)
                .build());
    }
}
