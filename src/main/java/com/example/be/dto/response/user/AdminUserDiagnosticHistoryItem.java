package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một dòng lịch sử làm bài thi chẩn đoán của học viên trong màn hình Chi tiết tài khoản (UC A02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDiagnosticHistoryItem {
    private Long attemptId;
    private String testTitle;
    private Integer targetScore;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationSeconds;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Integer predictedScore;
}
