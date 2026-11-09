package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Phản hồi chi tiết lời giải thích câu hỏi TOEIC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExplanationResponse {

    private String transcript;
    private String translation;
    private String clue;
    private String analysis;
    private List<QuestionVocabularyItemResponse> vocabulary;
}

