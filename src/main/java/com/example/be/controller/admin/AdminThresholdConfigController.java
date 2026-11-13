package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.setting.CopyThresholdsFromAimRequest;
import com.example.be.dto.request.setting.SaveAbilityRulesBatchRequest;
import com.example.be.dto.request.setting.SavePartThresholdsBatchRequest;
import com.example.be.dto.response.setting.AimAbilityRuleMatrixResponse;
import com.example.be.dto.response.setting.AimPartThresholdMatrixResponse;
import com.example.be.dto.response.setting.ThresholdValidationResponse;
import com.example.be.service.setting.service.AdminThresholdConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản trị cấu hình Ngưỡng đánh giá Part và Quy tắc đánh giá Năng lực (A08 & A10).
 * Cho phép xem ma trận, lưu hàng loạt (Batch Upsert), kiểm tra tính hợp lệ và sao chép giữa các AIM.
 */
@RestController
@RequestMapping("/api/admin/threshold-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminThresholdConfigController {

    private final AdminThresholdConfigService thresholdConfigService;

    // =========================================================================
    // A08: CẤU HÌNH NGƯỠNG ĐÁNH GIÁ PART THEO AIM
    // =========================================================================

    @GetMapping("/parts")
    public ApiResponse<AimPartThresholdMatrixResponse> getPartThresholdMatrix(
            @RequestParam Long targetProfileId
    ) {
        return new ApiResponse<>(true, "Tải ma trận ngưỡng Part thành công",
                thresholdConfigService.getPartThresholdMatrix(targetProfileId));
    }

    @PostMapping("/parts/batch")
    public ApiResponse<AimPartThresholdMatrixResponse> savePartThresholdsBatch(
            @Valid @RequestBody SavePartThresholdsBatchRequest request
    ) {
        return new ApiResponse<>(true, "Cập nhật cấu hình ngưỡng Part thành công",
                thresholdConfigService.savePartThresholdsBatch(request));
    }

    @PostMapping("/parts/validate")
    public ApiResponse<ThresholdValidationResponse> validatePartThresholds(
            @Valid @RequestBody SavePartThresholdsBatchRequest request
    ) {
        ThresholdValidationResponse result = thresholdConfigService.validatePartThresholds(request);
        String msg = result.isValid() ? "Cấu hình ngưỡng Part hợp lệ" : "Phát hiện lỗi logic trong cấu hình Part";
        return new ApiResponse<>(result.isValid(), msg, result);
    }

    @PostMapping("/parts/copy")
    public ApiResponse<AimPartThresholdMatrixResponse> copyThresholdsFromAim(
            @Valid @RequestBody CopyThresholdsFromAimRequest request
    ) {
        return new ApiResponse<>(true, "Sao chép cấu hình từ AIM khác thành công",
                thresholdConfigService.copyThresholdsFromAim(request));
    }

    // =========================================================================
    // A10: CẤU HÌNH QUY TẮC ĐÁNH GIÁ NĂNG LỰC THEO AIM
    // =========================================================================

    @GetMapping("/abilities")
    public ApiResponse<AimAbilityRuleMatrixResponse> getAbilityRuleMatrix(
            @RequestParam Long targetProfileId
    ) {
        return new ApiResponse<>(true, "Tải ma trận quy tắc Năng lực thành công",
                thresholdConfigService.getAbilityRuleMatrix(targetProfileId));
    }

    @PostMapping("/abilities/batch")
    public ApiResponse<AimAbilityRuleMatrixResponse> saveAbilityRulesBatch(
            @Valid @RequestBody SaveAbilityRulesBatchRequest request
    ) {
        return new ApiResponse<>(true, "Cập nhật quy tắc đánh giá Năng lực thành công",
                thresholdConfigService.saveAbilityRulesBatch(request));
    }

    @PostMapping("/abilities/validate")
    public ApiResponse<ThresholdValidationResponse> validateAbilityRules(
            @Valid @RequestBody SaveAbilityRulesBatchRequest request
    ) {
        ThresholdValidationResponse result = thresholdConfigService.validateAbilityRules(request);
        String msg = result.isValid() ? "Quy tắc Năng lực hợp lệ" : "Phát hiện lỗi logic trong quy tắc Năng lực";
        return new ApiResponse<>(result.isValid(), msg, result);
    }
}

