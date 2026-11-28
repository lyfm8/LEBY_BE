package com.example.be.dto.response.content;

import com.example.be.dto.response.exam.ExamOptionResponse;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.enums.content.EMistakeSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi chi tiết một câu hỏi trong Sổ tay câu hỏi làm sai (UC15 & BR14).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeQuestionItemResponse {

    private Long questionId;
    private Long parentId;
    private Long partId;
    private Integer partNo;
    private String partName;
    private String section;
    private List<AbilityTag> abilities;

    private EMistakeSourceType sourceType;
    private String sourceLabel;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;

    private String title;
    private String instruction;
    private String passage;
    private String audioUrl;
    private String imageUrl;
    private Boolean isGroupQuestion;
    private List<ExamOptionResponse> options;

    private String lastUserAnswer;
    private String correctAnswer;
    private QuestionExplanationResponse explanation;

    /**
     * Nhãn kỹ năng (Ability) gắn với câu hỏi sai.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityTag {
        private Long abilityId;
        private String abilityName;
    }
}
