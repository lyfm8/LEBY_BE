package com.example.be.dto.response.exam;

import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu chi tiết một câu hỏi trong Phòng luyện tập tức thì (UC12).
 * Bao gồm đầy đủ ngữ cảnh câu hỏi cha, đáp án đúng và giải thích chi tiết để kiểm tra tức thì tại chỗ.
 */
@Getter
@Builder
public class PracticeRoomQuestionResponse {

    private Long practiceQuestionId;
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
    private String correctAnswer;
    private QuestionExplanationResponse explanation;
}
