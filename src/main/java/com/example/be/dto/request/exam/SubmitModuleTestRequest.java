package com.example.be.dto.request.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload nộp bài thi vượt ải mô-đun (UC13 Bước 9 & BR07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitModuleTestRequest {

    @NotNull(message = "Mã lượt thi không được để trống")
    private Long attemptId;

    @NotNull(message = "Thời gian làm bài thực tế không được để trống")
    @Min(value = 0, message = "Thời gian làm bài không được âm")
    @Max(value = 7200, message = "Thời gian làm bài vượt quá giới hạn tối đa")
    private Integer durationSeconds;

    /**
     * Cờ đánh dấu hệ thống tự động thu bài khi đồng hồ chạm 00:00 (BR07).
     */
    private Boolean isTimedOut;

    @NotNull(message = "Danh sách câu trả lời không được để trống")
    @Size(max = 200, message = "Số lượng câu trả lời không vượt quá 200 câu")
    @Valid
    private List<SubmitAnswerItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerItem {

        @NotNull(message = "Mã câu hỏi không được để trống")
        private Long questionId;

        /**
         * Đáp án học viên chọn dạng JSON {"selected":"A"} hoặc ký tự A/B/C/D (nullable nếu bỏ qua).
         */
        private String userAnswer;

        private Boolean isFlagged;
    }
}
