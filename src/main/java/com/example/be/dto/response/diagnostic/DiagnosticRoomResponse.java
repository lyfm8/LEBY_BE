package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ dữ liệu đề thi và trạng thái phiên làm bài trả về cho Phòng thi chẩn đoán (UC06).
 */
@Getter
@Builder
public class DiagnosticRoomResponse {
    /** ID lượt làm bài (DiagnosticAttempt). */
    private Long attemptId;
    /** ID đề thi chẩn đoán gốc (DiagnosticTest). */
    private Long testId;
    /** ID mục tiêu AIM gắn với lượt thi này. */
    private Long targetProfileId;
    /** Tiêu đề bài thi. */
    private String title;
    /** Mô tả bài thi. */
    private String description;
    /** Thời lượng giới hạn của bài thi (phút). */
    private Integer durationMinutes;
    /** Thời điểm bắt đầu làm bài trên server. */
    private LocalDateTime startedAt;
    /** Thời gian làm bài còn lại tính theo giây (đã trừ thời gian trôi qua khi resume). */
    private Long remainingSeconds;
    /** Tổng số câu hỏi trong bài thi. */
    private Integer totalQuestions;
    /** Danh sách câu hỏi đã sắp xếp theo orderNo. */
    private List<DiagnosticQuestionResponse> questions;
}
