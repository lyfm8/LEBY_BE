package com.example.be.service.setting.service;

import com.example.be.dto.request.setting.CopyThresholdsFromAimRequest;
import com.example.be.dto.request.setting.SaveAbilityRulesBatchRequest;
import com.example.be.dto.request.setting.SavePartThresholdsBatchRequest;
import com.example.be.dto.response.setting.AimAbilityRuleMatrixResponse;
import com.example.be.dto.response.setting.AimPartThresholdMatrixResponse;
import com.example.be.dto.response.setting.ThresholdValidationResponse;

/**
 * Service quản trị cấu hình Ngưỡng đánh giá Part và Quy tắc đánh giá Năng lực theo AIM (A08 & A10).
 */
public interface AdminThresholdConfigService {

    /**
     * Lấy ma trận cấu hình ngưỡng 7 Part theo mục tiêu AIM.
     *
     * @param targetProfileId ID của TargetProfile (AIM)
     * @return AimPartThresholdMatrixResponse Ma trận 7 Part kèm ngưỡng Pass / Confirm
     */
    AimPartThresholdMatrixResponse getPartThresholdMatrix(Long targetProfileId);

    /**
     * Lưu hàng loạt (Batch Upsert) cấu hình ngưỡng 7 Part cho một mục tiêu AIM.
     *
     * @param request Dữ liệu 7 Part threshold cần cập nhật
     * @return AimPartThresholdMatrixResponse Ma trận sau khi lưu
     */
    AimPartThresholdMatrixResponse savePartThresholdsBatch(SavePartThresholdsBatchRequest request);

    /**
     * Kiểm tra tính hợp lệ logic toán học của bộ ngưỡng 7 Part (Confirm < Pass).
     *
     * @param request Dữ liệu ngưỡng cần kiểm tra
     * @return ThresholdValidationResponse Kết quả kiểm tra kèm danh sách lỗi chi tiết nếu có
     */
    ThresholdValidationResponse validatePartThresholds(SavePartThresholdsBatchRequest request);

    /**
     * Lấy ma trận cấu hình quy tắc đánh giá toàn bộ Năng lực (Ability) theo mục tiêu AIM.
     *
     * @param targetProfileId ID của TargetProfile (AIM)
     * @return AimAbilityRuleMatrixResponse Ma trận các Ability kèm ngưỡng stableMin
     */
    AimAbilityRuleMatrixResponse getAbilityRuleMatrix(Long targetProfileId);

    /**
     * Lưu hàng loạt (Batch Upsert) quy tắc đánh giá Năng lực cho một mục tiêu AIM.
     *
     * @param request Dữ liệu các Ability rule cần cập nhật
     * @return AimAbilityRuleMatrixResponse Ma trận sau khi lưu
     */
    AimAbilityRuleMatrixResponse saveAbilityRulesBatch(SaveAbilityRulesBatchRequest request);

    /**
     * Kiểm tra tính hợp lệ của bộ quy tắc Năng lực (0 < stableMin <= 100).
     *
     * @param request Dữ liệu quy tắc cần kiểm tra
     * @return ThresholdValidationResponse Kết quả kiểm tra kèm danh sách lỗi chi tiết nếu có
     */
    ThresholdValidationResponse validateAbilityRules(SaveAbilityRulesBatchRequest request);

    /**
     * Sao chép toàn bộ cấu hình (cả Part Thresholds và Ability Rules) từ một AIM nguồn sang AIM đích.
     *
     * @param request Chứa sourceProfileId và targetProfileId
     * @return AimPartThresholdMatrixResponse Ma trận Part của AIM đích sau khi sao chép
     */
    AimPartThresholdMatrixResponse copyThresholdsFromAim(CopyThresholdsFromAimRequest request);
}

