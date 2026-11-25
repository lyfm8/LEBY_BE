package com.example.be.dto.request.diagnostic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu nộp bài kiểm tra chẩn đoán.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubmitDiagnosticRequest {

    /** ID lượt làm bài cần nộp. */
    @NotNull(message = "attemptId không được để trống")
    private Long attemptId;

    /** ID mục tiêu AIM học viên đã chọn (phục vụ sinh lộ trình ngay sau chấm). */
    @NotNull(message = "targetProfileId không được để trống")
    private Long targetProfileId;

    /** Danh sách đáp án của từng câu hỏi. Giới hạn 500 phần tử chống DoS. */
    @Size(max = 500, message = "Danh sách câu trả lời không được vượt quá 500 câu")
    private List<@Valid AnswerItem> answers;

    /** Thời gian làm bài thực tế tính bằng giây (tối đa 4 giờ = 14400s). */
    @Min(value = 0, message = "Thời gian làm bài không được nhỏ hơn 0")
    @Max(value = 14400, message = "Thời gian làm bài vượt quá giới hạn cho phép")
    private Integer durationSeconds;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnswerItem {
        @NotNull(message = "questionId không được để trống")
        private Long questionId;

        /** Đáp án học viên chọn, dạng JSON string, ví dụ: {\"selected\":\"A\"}. */
        private String userAnswer;
    }
}
