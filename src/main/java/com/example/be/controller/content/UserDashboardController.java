package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.dashboard.UserDashboardSummaryResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.dashboard.service.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API tổng hợp cho màn hình Dashboard học tập cá nhân của học viên (UC08).
 *
 * <p>Endpoint:
 * <pre>
 *   GET /api/users/dashboard/summary — Lấy tổng quan tiến độ, Streak, mô-đun đang học và biểu đồ tuần
 * </pre>
 */
@RestController
@RequestMapping("/api/users/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    /**
     * Lấy toàn bộ dữ liệu tổng hợp cho Dashboard học tập cá nhân của người dùng đang đăng nhập.
     *
     * @param currentUser Người dùng hiện tại trích xuất từ JWT SecurityContext (chống IDOR tuyệt đối)
     * @return {@link ApiResponse} bọc {@link UserDashboardSummaryResponse}
     */
    @GetMapping("/summary")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<UserDashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal User currentUser
    ) {
        // SECURITY: Luôn dùng currentUser.getId() từ SecurityContext, không nhận userId từ phía client
        UserDashboardSummaryResponse summary = userDashboardService.getStudentDashboardSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<UserDashboardSummaryResponse>builder()
                .success(true)
                .message("Tải dữ liệu bảng điều khiển học tập thành công")
                .data(summary)
                .build());
    }
}
