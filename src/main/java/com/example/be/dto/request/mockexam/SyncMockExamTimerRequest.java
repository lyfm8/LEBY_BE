package com.example.be.dto.request.mockexam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu đồng bộ số giây làm bài còn lại trong phòng thi thử TOEIC 200 câu (UC17 Luồng 7a).
 */
@Getter
@Setter
@NoArgsConstructor
public class SyncMockExamTimerRequest {

    /** ID lượt thi thử đang diễn ra. */
    @NotNull(message = "attemptId không được để trống")
    private Long attemptId;

    /** Số giây đếm ngược còn lại trên đồng hồ phòng thi (tối đa 4 giờ = 14400s). */
    @NotNull(message = "remainingSeconds không được để trống")
    @Min(value = 0, message = "Thời gian còn lại không được nhỏ hơn 0")
    @Max(value = 14400, message = "Thời gian còn lại vượt quá giới hạn cho phép")
    private Integer remainingSeconds;
}
