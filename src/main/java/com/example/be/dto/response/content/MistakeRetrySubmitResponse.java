package com.example.be.dto.response.content;

import com.example.be.dto.response.question.QuestionExplanationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO phản hồi kết quả chấm điểm toàn bộ phiên làm lại các câu sai kèm lời giải chi tiết BR14 (UC15 Luồng 5a).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeRetrySubmitResponse {

    private Integer totalQuestions;
    private Integer answeredCount;
    private Integer correctCount;
    private Integer accuracyPercent;
    private List<QuestionRetryResult> results;

    /**
     * Kết quả chấm chi tiết cho từng câu hỏi trong phiên làm lại.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionRetryResult {
        private Long questionId;
        private String selectedAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private QuestionExplanationResponse explanation;
    }
}
