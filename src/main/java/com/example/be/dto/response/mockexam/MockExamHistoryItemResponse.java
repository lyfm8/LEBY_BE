package com.example.be.dto.response.mockexam;

import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Một dòng lịch sử làm bài thi thử TOEIC 200 câu của học viên (UC17 & UC18).
 */
@Getter
@Builder
public class MockExamHistoryItemResponse {

    private Long attemptId;
    private Long testId;
    private String testTitle;
    private EExamTag examTag;
    private EAttemptStatus status;
    private LocalDateTime completedAt;
    private Integer durationSeconds;
    private Integer listeningScore;
    private Integer readingScore;
    private Integer totalToeicScore;
    private Integer totalCorrect;
    private Integer totalQuestions;
    private Boolean isAimAchieved;
    private Boolean canViewReview;
}
