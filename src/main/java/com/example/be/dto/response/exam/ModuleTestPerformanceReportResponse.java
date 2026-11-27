package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Báo cáo kết quả chi tiết của một lần thi vượt ải mô-đun (UC14).
 */
@Getter
@Builder
public class ModuleTestPerformanceReportResponse {

    private Long attemptId;
    private Long moduleId;
    private String moduleTitle;
    private String completedAt;
    private Integer score;
    private Integer maxScore;
    private Integer passScore;

    @JsonProperty("isPassed")
    private Boolean isPassed;

    private Integer diffFromPass;
    private AiFeedbackResponse aiFeedback;
    private MetricSummaryResponse metrics;
    private List<AttemptHistoryItemResponse> history;
    private Long nextModuleId;

    @JsonProperty("isRoadmapCompleted")
    private Boolean isRoadmapCompleted;

    @Getter
    @Builder
    public static class AiFeedbackResponse {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class MetricSummaryResponse {
        private Integer accuracyRate;
        private Integer correctAnswers;
        private Integer totalQuestions;
        private String durationFormatted;
        private String speedComparison;
        private String competencyStatus;
        private Integer gapResolvedPercent;
    }

    @Getter
    @Builder
    public static class AttemptHistoryItemResponse {
        private Integer attemptNumber;
        private Long attemptId;

        @JsonProperty("isLatest")
        private Boolean isLatest;

        private String completedAt;
        private Integer score;
        private Integer maxScore;

        @JsonProperty("isPassed")
        private Boolean isPassed;

        private String durationFormatted;
    }
}
