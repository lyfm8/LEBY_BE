package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ dữ liệu phiên xem lại bài thi chẩn đoán đã hoàn thành (UC07 - BR14).
 */
@Getter
@Builder
public class DiagnosticReviewResponse {
    /** ID lượt làm bài chẩn đoán. */
    private Long attemptId;
    /** ID đề thi chẩn đoán gốc. */
    private Long testId;
    /** Tiêu đề bài thi chẩn đoán. */
    private String testTitle;
    /** Thời điểm học viên hoàn thành và nộp bài. */
    private LocalDateTime completedAt;
    /** Tổng số câu hỏi trong đề thi. */
    private Integer totalQuestions;
    /** Tổng số câu học viên trả lời đúng. */
    private Integer correctCount;
    /** Danh sách chi tiết câu hỏi kèm đáp án đã chọn, đáp án đúng và giải thích. */
    private List<DiagnosticReviewQuestionResponse> questions;
}
