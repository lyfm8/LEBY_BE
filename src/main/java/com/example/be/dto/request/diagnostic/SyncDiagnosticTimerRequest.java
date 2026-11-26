package com.example.be.dto.request.diagnostic;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu đồng bộ số giây làm bài còn lại của phiên thi chẩn đoán đang diễn ra (UC06).
 */
@Getter
@Setter
@NoArgsConstructor
public class SyncDiagnosticTimerRequest {

    /** ID lượt làm bài cần cập nhật bộ đếm thời gian. */
    @NotNull(message = "ID lượt làm bài không được để trống")
    private Long attemptId;

    /** Số giây làm bài còn lại từ đồng hồ phòng thi (>= 0). */
    @NotNull(message = "Thời gian còn lại không được để trống")
    @Min(value = 0, message = "Thời gian còn lại không được nhỏ hơn 0")
    private Integer remainingSeconds;
}
