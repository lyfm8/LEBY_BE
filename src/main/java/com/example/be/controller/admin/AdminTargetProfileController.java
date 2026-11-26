package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.TargetProfileRequest;
import com.example.be.dto.response.user.TargetProfileResponse;
import com.example.be.security.Roles;
import com.example.be.service.user.service.TargetProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller quản trị danh mục Mục tiêu điểm TOEIC (TargetProfile / AIM).
 * Bảo vệ phân quyền RBAC chặt chẽ bằng @PreAuthorize và kiểm tra đầu vào bằng @Valid.
 */
@RestController
@RequestMapping("/api/admin/target-profiles")
@RequiredArgsConstructor
public class AdminTargetProfileController {

    private final TargetProfileService targetProfileService;

    /**
     * Lấy danh sách toàn bộ mốc điểm mục tiêu AIM (cho phép cả Teacher và Admin tra cứu cấu hình).
     */
    @GetMapping
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ApiResponse<List<TargetProfileResponse>> getAllProfiles() {
        return new ApiResponse<>(true, "Success", targetProfileService.getAllProfiles());
    }

    /**
     * Tạo mới một mốc mục tiêu AIM (chỉ ADMIN được phép thao tác ghi).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    public ApiResponse<TargetProfileResponse> createProfile(@Valid @RequestBody TargetProfileRequest request) {
        return new ApiResponse<>(true, "Created successfully", targetProfileService.createProfile(request));
    }

    /**
     * Cập nhật thông tin mốc mục tiêu AIM theo ID (chỉ ADMIN được phép).
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ApiResponse<TargetProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody TargetProfileRequest request
    ) {
        return new ApiResponse<>(true, "Updated successfully", targetProfileService.updateProfile(id, request));
    }

    /**
     * Xóa mốc mục tiêu AIM theo ID (chỉ ADMIN được phép).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ApiResponse<Void> deleteProfile(@PathVariable Long id) {
        targetProfileService.deleteProfile(id);
        return new ApiResponse<>(true, "Deleted successfully");
    }
}
