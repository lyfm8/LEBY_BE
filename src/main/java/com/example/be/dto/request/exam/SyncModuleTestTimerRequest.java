package com.example.be.dto.request.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload đồng bộ số giây làm bài còn lại lên máy chủ khi tạm dừng, định kỳ hoặc thoát trang (UC13 Luồng 7a).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncModuleTestTimerRequest {

    @NotNull(message = "Mã lượt thi không được để trống")
    private Long attemptId;

    @NotNull(message = "Số giây còn lại không được để trống")
    @Min(value = 0, message = "Số giây còn lại không được âm")
    @Max(value = 7200, message = "Số giây còn lại vượt quá giới hạn tối đa")
    private Integer remainingSeconds;
}
