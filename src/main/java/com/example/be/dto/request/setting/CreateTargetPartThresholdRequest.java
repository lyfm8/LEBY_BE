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
public class CreateTargetPartThresholdRequest {

    @NotNull(message = "Target Profile ID không được để trống")
    private Long targetProfileId;

    @NotNull(message = "Part ID không được để trống")
    private Long partId;

    @NotNull(message = "Điểm đạt chuẩn (passScore) không được để trống")
    @Min(value = 0, message = "Điểm đạt chuẩn tối thiểu là 0%")
    @Max(value = 100, message = "Điểm đạt chuẩn tối đa là 100%")
    private Float passScore;

    @Min(value = 0, message = "Điểm cận chuẩn tối thiểu là 0%")
    @Max(value = 100, message = "Điểm cận chuẩn tối đa là 100%")
    private Float confirmingScore;

    @Min(value = 0, message = "Điểm mất gốc tối thiểu là 0%")
    @Max(value = 100, message = "Điểm mất gốc tối đa là 100%")
    private Float fullPartThreshold;
}
