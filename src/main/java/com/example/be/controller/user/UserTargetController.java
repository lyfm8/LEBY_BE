package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.SelectTargetRequest;
import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.user.service.UserTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller xử lý nghiệp vụ Chọn / Thay đổi mục tiêu điểm TOEIC (AIM) cho học viên (UC05).
 *
 * <p>Endpoints:
 * <pre>
 *   GET  /api/targets        — Lấy danh sách 5 mốc AIM đang hoạt động
 *   POST /api/targets/select — Xác nhận chọn mới hoặc đổi mục tiêu AIM
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class UserTargetController {

    private final UserTargetService userTargetService;

    /**
     * Lấy danh sách các mốc mục tiêu TOEIC đang khả dụng cho học viên lựa chọn.
     *
     * @return Danh sách {@link UserTargetProfileResponse}
     */
    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<List<UserTargetProfileResponse>>> getTargets() {
        List<UserTargetProfileResponse> targets = userTargetService.getAllActiveTargets();
        return ResponseEntity.ok(ApiResponse.<List<UserTargetProfileResponse>>builder()
                .success(true)
                .message("Tải danh sách mục tiêu TOEIC thành công")
                .data(targets)
                .build());
    }

    /**
     * Xác nhận chọn mục tiêu lần đầu hoặc thay đổi mục tiêu AIM hiện tại.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @param request     DTO chứa ID mốc mục tiêu muốn thiết lập
     * @return {@link SelectTargetResponse} kèm chỉ dẫn điều hướng (GO_DIAGNOSTIC | GO_LEARNING_PATH)
     */
    @PostMapping("/select")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<SelectTargetResponse>> selectTarget(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SelectTargetRequest request
    ) {
        SelectTargetResponse response = userTargetService.selectTarget(currentUser.getId(), request.getTargetProfileId());
        String message = SelectTargetResponse.ACTION_GO_LEARNING_PATH.equals(response.getRedirectAction())
                ? "Đổi mục tiêu AIM thành công. Lộ trình học mới đã được khởi tạo"
                : "Thiết lập mục tiêu AIM thành công";

        return ResponseEntity.ok(ApiResponse.<SelectTargetResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }
}
