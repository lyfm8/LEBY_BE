package com.example.be.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO tóm tắt tình trạng bài thi chẩn đoán đầu vào gần nhất của học viên (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardDiagnosticDto {

    @JsonProperty("hasCompletedDiagnostic")
    private Boolean hasCompletedDiagnostic;

    @JsonProperty("hasInProgressDiagnostic")
    private Boolean hasInProgressDiagnostic;

    private Long latestAttemptId;
    private Integer predictedScore;
    private Integer listeningScore;
    private Integer readingScore;
    private LocalDateTime completedAt;
}
