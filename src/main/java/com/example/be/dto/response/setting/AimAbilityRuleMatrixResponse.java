package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO phản hồi ma trận quy tắc đánh giá Năng lực (Ability) theo AIM đã chọn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AimAbilityRuleMatrixResponse {

    private Long targetProfileId;
    private Integer aimScore;
    private String aimDescription;
    private List<AbilityRuleRowResponse> rows;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbilityRuleRowResponse {
        private Long id;
        private Long abilityId;
        private String abilityName;
        private String abilityDescription;
        private Long partId;
        private String partName;
        private String section;
        private Float stableMin;
    }
}

