package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi mục từ vựng học thuật trong giải thích câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVocabularyItemResponse {

    private String word;
    private String type;
    private String meaning;
}

