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
public class ModuleThresholdItemDto {

    @NotNull(message = "Module ID không được để trống")
    private Long moduleId;

    @NotNull(message = "Target Profile ID không được để trống")
    private Long targetProfileId;

    @Min(value = 0, message = "Điểm đỗ phải từ 0 trở lên")
    @Max(value = 100, message = "Điểm đỗ tối đa là 100")
    private Integer passScore;
}
