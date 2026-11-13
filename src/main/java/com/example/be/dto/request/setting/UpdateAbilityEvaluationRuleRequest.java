package com.example.be.dto.request.setting;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAbilityEvaluationRuleRequest {

    @NotNull(message = "Ngưỡng vững vàng (stableMin) không được để trống")
    @Min(value = 0, message = "Ngưỡng vững vàng tối thiểu là 0%")
    @Max(value = 100, message = "Ngưỡng vững vàng tối đa là 100%")
    private Float stableMin;

    @NotNull(message = "Ngưỡng yếu (weakMax) không được để trống")
    @Min(value = 0, message = "Ngưỡng yếu tối thiểu là 0%")
    @Max(value = 100, message = "Ngưỡng yếu tối đa là 100%")
    private Float weakMax;

    @NotNull(message = "Số bằng chứng tối thiểu không được để trống")
    @Min(value = 1, message = "Số bằng chứng tối thiểu là 1 câu")
    @Max(value = 50, message = "Số bằng chứng tối đa là 50 câu")
    private Integer minEvidenceCount;
}
