package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityEvaluationRuleResponse {
    private Long id;
    private Long abilityId;
    private String abilityName;
    private Long partId;
    private String partName;
    private Float stableMin;
    private Float developingMax;
    private Float weakMax;
    private Integer minEvidenceCount;
}
