package com.example.be.dto.request.setting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO gửi yêu cầu lưu ma trận quy tắc đánh giá Năng lực theo AIM (A10).
 * 2 mức đánh giá (Weak / Stable) tương ứng 1 mốc ngưỡng duy nhất (stableMin).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveAbilityRulesBatchRequest {

    @NotNull(message = "ID mục tiêu AIM không được để trống")
    private Long targetProfileId;

    @Valid
    @NotNull(message = "Danh sách quy tắc Năng lực không được để trống")
    private List<AbilityRuleItem> rules;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbilityRuleItem {

        @NotNull(message = "ID Năng lực (Ability) không được để trống")
        private Long abilityId;

        @NotNull(message = "Ngưỡng Ổn định (Stable) không được để trống")
        @Min(value = 0, message = "Ngưỡng Ổn định tối thiểu là 0%")
        @Max(value = 100, message = "Ngưỡng Ổn định tối đa là 100%")
        private Float stableMin;
    }
}

