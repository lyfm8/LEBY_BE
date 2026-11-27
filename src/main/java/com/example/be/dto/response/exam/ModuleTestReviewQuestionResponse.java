package com.example.be.dto.response.exam;

import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu chi tiết một câu hỏi trong chế độ Xem lại bài thi vượt ải mô-đun (UC14 Luồng 6a).
 */
@Getter
@Builder
public class ModuleTestReviewQuestionResponse {

    private Long questionId;
    private Integer orderNo;
    private Long parentId;
    private Long partId;
    private Integer partNo;
    private String partName;
    private String section;
    private String title;
    private String instruction;
    private String passage;
    private String audioUrl;
    private String imageUrl;

    @JsonProperty("isGroupQuestion")
    private Boolean isGroupQuestion;

    private List<ExamOptionResponse> options;
    private String userAnswer;
    private String correctAnswer;

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    @JsonProperty("isFlagged")
    private Boolean isFlagged;

    private QuestionExplanationResponse explanation;
}
