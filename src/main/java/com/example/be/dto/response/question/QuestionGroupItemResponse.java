package com.example.be.dto.response.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Phản hồi chi tiết từng câu hỏi con trong chùm câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGroupItemResponse {

    private Long id;
    private String name;
    private Integer orderNo;
    private String text;
    private List<QuestionOptionResponse> options;
    private String correctKey;
    private Integer difficulty;
    private QuestionExplanationResponse explanation;
    private List<QuestionMediaResponse> medias;
    private List<QuestionAbilityResponse> abilities;
}

