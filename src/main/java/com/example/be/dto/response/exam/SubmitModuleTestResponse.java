package com.example.be.dto.response.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả tóm tắt ngay sau khi nộp bài thi vượt ải mô-đun (UC13 -> UC14).
 */
@Getter
@Builder
public class SubmitModuleTestResponse {

    private Long attemptId;
    private Long moduleId;
    private Integer score;
    private Integer passScore;

    @JsonProperty("isPassed")
    private Boolean isPassed;

    private Integer correctCount;
    private Integer totalQuestions;
    private Long nextModuleId;

    @JsonProperty("isRoadmapCompleted")
    private Boolean isRoadmapCompleted;
}
