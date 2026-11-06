package com.example.be.dto.request.content;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cập nhật nhanh các tham số động của Module (Điểm đỗ và Tỷ lệ hoàn thành).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModuleThresholdsRequest {

    @NotNull(message = "Điểm đỗ mặc định không được để trống")
    @Min(value = 50, message = "Điểm đỗ mặc định tối thiểu là 50")
    @Max(value = 100, message = "Điểm đỗ mặc định tối đa là 100")
    private Integer defaultPassScore;

    @NotNull(message = "Tỷ lệ hoàn thành bài giảng tối thiểu không được để trống")
    @Min(value = 50, message = "Tỷ lệ hoàn thành tối thiểu là 50%")
    @Max(value = 100, message = "Tỷ lệ hoàn thành tối đa là 100%")
    private Integer requiredCompletionRate;
}

