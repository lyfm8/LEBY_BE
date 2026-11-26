package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một dòng kết quả bài thi thử 200 câu (FULL_MOCK_TEST) đã nộp của học viên (UC A02 - Bước 6).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserMockTestHistoryItem {
    private Long attemptId;
    private String testTitle;
    private LocalDateTime completedAt;
    private Integer durationSeconds;
    private Integer listeningCorrect;
    private Integer listeningTotal;
    private Integer listeningScore;
    private Integer readingCorrect;
    private Integer readingTotal;
    private Integer readingScore;
    private Integer totalScore;
}
