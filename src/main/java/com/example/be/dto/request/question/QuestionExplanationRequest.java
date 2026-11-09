package com.example.be.dto.request.question;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cấu trúc lời giải thích chi tiết cho câu hỏi TOEIC theo chuẩn Phụ lục 1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExplanationRequest {

    private String transcript;
    private String translation;
    private String clue;
    private String analysis;
    private List<@Valid QuestionVocabularyItemRequest> vocabulary;
}

