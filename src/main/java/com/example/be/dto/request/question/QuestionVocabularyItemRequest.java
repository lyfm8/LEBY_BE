package com.example.be.dto.request.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mục từ vựng quan trọng kèm theo lời giải thích câu hỏi TOEIC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVocabularyItemRequest {

    private String word;
    private String type;
    private String meaning;
}

