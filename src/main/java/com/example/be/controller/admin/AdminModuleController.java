package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.content.CreateModuleRequest;
import com.example.be.dto.request.content.ReorderModulesRequest;
import com.example.be.dto.request.content.UpdateModuleRequest;
import com.example.be.dto.request.content.UpdateModuleThresholdsRequest;
import com.example.be.dto.response.content.ModuleDetailResponse;
import com.example.be.dto.response.content.ModuleSummaryResponse;
import com.example.be.service.content.service.AdminModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller phục vụ quản lý Module học tập dành cho Quản trị viên và Giáo viên (A05).
 * URL prefix: /api/admin/modules
 */
@RestController
@RequestMapping("/api/admin/modules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminModuleController {

    private final AdminModuleService adminModuleService;

    /**
     * Lấy toàn bộ danh sách Module kèm bài học con theo thứ tự hiển thị sư phạm.
     */
    @GetMapping
    public ApiResponse<List<ModuleSummaryResponse>> getAllModules() {
        return new ApiResponse<>(true, "Lấy danh sách Module thành công", adminModuleService.getAllModules());
    }

    /**
     * Lấy thông tin chi tiết của một Module theo ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<ModuleDetailResponse> getModuleById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Lấy chi tiết Module thành công", adminModuleService.getModuleById(id));
    }

    /**
     * Tạo mới một Module học tập.
     */
    @PostMapping
    public ApiResponse<ModuleDetailResponse> createModule(@Valid @RequestBody CreateModuleRequest request) {
        return new ApiResponse<>(true, "Tạo Module thành công", adminModuleService.createModule(request));
    }

    /**
     * Cập nhật thông tin toàn diện của một Module.
     */
    @PutMapping("/{id}")
    public ApiResponse<ModuleDetailResponse> updateModule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModuleRequest request
    ) {
        return new ApiResponse<>(true, "Cập nhật Module thành công", adminModuleService.updateModule(id, request));
    }

    /**
     * Cập nhật nhanh các thông số động của Module (Điểm đỗ và Tỷ lệ bài giảng hoàn thành).
     */
    @PatchMapping("/{id}/thresholds")
    public ApiResponse<ModuleDetailResponse> updateModuleThresholds(
            @PathVariable Long id,
            @Valid @RequestBody UpdateModuleThresholdsRequest request
    ) {
        return new ApiResponse<>(true, "Cập nhật thông số Module thành công", adminModuleService.updateModuleThresholds(id, request));
    }

    /**
     * Xóa Module học tập (chặn xóa nếu có học viên theo học).
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteModule(@PathVariable Long id) {
        adminModuleService.deleteModule(id);
        return new ApiResponse<>(true, "Xóa Module thành công");
    }

    /**
     * Sắp xếp lại thứ tự sư phạm của danh sách Module sau khi kéo thả.
     */
    @PatchMapping("/reorder")
    public ApiResponse<Void> reorderModules(@Valid @RequestBody ReorderModulesRequest request) {
        adminModuleService.reorderModules(request);
        return new ApiResponse<>(true, "Sắp xếp lại thứ tự Module thành công");
    }
}

