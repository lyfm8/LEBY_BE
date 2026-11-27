package com.example.be.dto.request.mockexam;

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
 * Yêu cầu nộp bài thi thử TOEIC chuẩn ETS 200 câu để chấm điểm quy đổi 990 (UC17).
 */
@Getter
@Setter
@NoArgsConstructor
public class SubmitMockExamRequest {

    /** ID lượt làm bài cần nộp. */
    @NotNull(message = "attemptId không được để trống")
    private Long attemptId;

    /** ID mục tiêu AIM học viên đang theo đuổi (tùy chọn). */
    private Long targetProfileId;

    /** Danh sách đáp án của học viên (giới hạn 250 phần tử chống Payload DoS). */
    @Size(max = 250, message = "Danh sách câu trả lời không được vượt quá 250 câu")
    private List<@Valid AnswerItem> answers;

    /** Thời gian làm bài thực tế tính bằng giây (tối đa 4 giờ = 14400s). */
    @Min(value = 0, message = "Thời gian làm bài không được nhỏ hơn 0")
    @Max(value = 14400, message = "Thời gian làm bài vượt quá giới hạn cho phép")
    private Integer durationSeconds;

    /** Đánh dấu hệ thống tự động thu bài khi đồng hồ chạm 00:00 (BR07). */
    private Boolean isTimedOut;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnswerItem {

        /** ID câu hỏi trong đề thi. */
        @NotNull(message = "questionId không được để trống")
        private Long questionId;

        /** Đáp án học viên chọn dạng JSON string, ví dụ: {"selected":"A"}. */
        @Size(max = 100, message = "Đáp án vượt quá độ dài cho phép")
        private String userAnswer;

        /** Cờ đánh dấu xem lại trong phòng thi. */
        private Boolean isFlagged;
    }
}
