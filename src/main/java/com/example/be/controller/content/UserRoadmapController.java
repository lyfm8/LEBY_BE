package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.content.service.UserRoadmapService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API Lộ trình học cá nhân (UC09) và Xem chi tiết Module (UC10) cho học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/users/roadmap                   — Lấy lộ trình học cá nhân hiện hành (UC09)
 *   GET /api/users/roadmap/modules/{moduleId} — Lấy chi tiết một Module đã mở khóa (UC10)
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/roadmap")
@RequiredArgsConstructor
public class UserRoadmapController {

    private final UserRoadmapService userRoadmapService;

    /**
     * Lấy toàn bộ lộ trình học tập cá nhân hóa hiện hành của học viên đang đăng nhập.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @return {@link ApiResponse} chứa {@link RoadmapResponse} hoặc {@code null} nếu chưa có lộ trình
     */
    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<RoadmapResponse>> getRoadmap(
            @AuthenticationPrincipal User currentUser
    ) {
        RoadmapResponse roadmap = userRoadmapService.getCurrentUserRoadmap(currentUser.getId());
        String message = roadmap != null
                ? "Tải lộ trình học cá nhân thành công"
                : "Học viên chưa có lộ trình học tập";

        return ResponseEntity.ok(ApiResponse.<RoadmapResponse>builder()
                .success(true)
                .message(message)
                .data(roadmap)
                .build());
    }

    /**
     * Lấy thông tin chi tiết của một Module thuộc lộ trình học hiện hành của học viên.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @param moduleId    ID của Module cần xem (bắt buộc là số nguyên dương)
     * @return {@link ApiResponse} chứa {@link UserModuleDetailResponse}
     */
    @GetMapping("/modules/{moduleId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<UserModuleDetailResponse>> getModuleDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId
    ) {
        UserModuleDetailResponse detail = userRoadmapService.getModuleDetailForUser(currentUser.getId(), moduleId);
        return ResponseEntity.ok(ApiResponse.<UserModuleDetailResponse>builder()
                .success(true)
                .message("Tải thông tin chi tiết mô-đun thành công")
                .data(detail)
                .build());
    }
}
