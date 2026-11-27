package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ dữ liệu chế độ Xem lại bài thi vượt ải đã nộp (UC14 Luồng 6a).
 */
@Getter
@Builder
public class ModuleTestReviewResponse {

    private Long attemptId;
    private Long moduleId;
    private String moduleTitle;
    private String testTitle;
    private LocalDateTime completedAt;
    private Integer score;
    private Integer passScore;

    @JsonProperty("isPassed")
    private Boolean isPassed;

    private Integer totalQuestions;
    private Integer correctCount;
    private List<ModuleTestReviewQuestionResponse> questions;
}
