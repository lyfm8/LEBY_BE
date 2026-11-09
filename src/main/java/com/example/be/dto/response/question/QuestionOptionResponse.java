package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi thông tin phương án trả lời của câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionResponse {

    private String key;
    private String text;
}

