package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.setting.BatchUpdateModuleThresholdRequest;
import com.example.be.dto.response.setting.ModuleTargetThresholdResponse;
import com.example.be.dto.response.setting.ModuleThresholdMatrixResponse;
import com.example.be.service.setting.service.ModuleTargetThresholdService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/module-target-thresholds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminModuleTargetThresholdController {

    private final ModuleTargetThresholdService thresholdService;

    @GetMapping("/matrix")
    public ApiResponse<ModuleThresholdMatrixResponse> getMatrix(@RequestParam(required = false) Long partId) {
        return new ApiResponse<>(true, "Lấy ma trận ngưỡng điểm đỗ Module thành công",
                thresholdService.getMatrix(partId));
    }

    @PostMapping("/batch")
    public ApiResponse<List<ModuleTargetThresholdResponse>> batchUpdate(
            @Valid @RequestBody BatchUpdateModuleThresholdRequest request) {
        return new ApiResponse<>(true, "Cập nhật hàng loạt ngưỡng điểm đỗ thành công",
                thresholdService.batchUpsert(request));
    }

    @PutMapping("/single")
    public ApiResponse<ModuleTargetThresholdResponse> updateSingle(
            @RequestParam Long moduleId,
            @RequestParam Long targetProfileId,
            @RequestParam(required = false) @Min(value = 0, message = "Điểm đỗ tối thiểu là 0") @Max(value = 100, message = "Điểm đỗ tối đa là 100") Integer passScore) {
        return new ApiResponse<>(true, "Cập nhật ngưỡng điểm đỗ thành công",
                thresholdService.upsertSingle(moduleId, targetProfileId, passScore));
    }

    @DeleteMapping("/reset")
    public ApiResponse<Void> resetToDefault(
            @RequestParam Long moduleId,
            @RequestParam Long targetProfileId) {
        thresholdService.resetToDefault(moduleId, targetProfileId);
        return new ApiResponse<>(true, "Khôi phục điểm đỗ mặc định thành công");
    }
}
