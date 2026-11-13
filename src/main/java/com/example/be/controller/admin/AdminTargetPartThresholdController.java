package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.setting.CreateTargetPartThresholdRequest;
import com.example.be.dto.request.setting.UpdateTargetPartThresholdRequest;
import com.example.be.dto.response.setting.TargetPartThresholdResponse;
import com.example.be.service.setting.service.TargetPartThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/target-part-thresholds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminTargetPartThresholdController {

    private final TargetPartThresholdService thresholdService;

    @GetMapping
    public ApiResponse<List<TargetPartThresholdResponse>> getAllThresholds() {
        return new ApiResponse<>(true, "Lấy danh sách ngưỡng điểm Part thành công", thresholdService.getAllThresholds());
    }

    @GetMapping("/by-profile/{targetProfileId}")
    public ApiResponse<List<TargetPartThresholdResponse>> getThresholdsByProfile(@PathVariable Long targetProfileId) {
        return new ApiResponse<>(true, "Lấy danh sách ngưỡng điểm Part theo Profile thành công", thresholdService.getThresholdsByProfile(targetProfileId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TargetPartThresholdResponse> getThresholdById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Lấy chi tiết ngưỡng điểm Part thành công", thresholdService.getThresholdById(id));
    }

    @PostMapping
    public ApiResponse<TargetPartThresholdResponse> createThreshold(
            @Valid @RequestBody CreateTargetPartThresholdRequest request) {
        return new ApiResponse<>(true, "Tạo mới cấu hình ngưỡng điểm Part thành công", thresholdService.createThreshold(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TargetPartThresholdResponse> updateThreshold(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTargetPartThresholdRequest request) {
        return new ApiResponse<>(true, "Cập nhật cấu hình ngưỡng điểm Part thành công", thresholdService.updateThreshold(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteThreshold(@PathVariable Long id) {
        thresholdService.deleteThreshold(id);
        return new ApiResponse<>(true, "Xóa cấu hình ngưỡng điểm Part thành công");
    }
}
