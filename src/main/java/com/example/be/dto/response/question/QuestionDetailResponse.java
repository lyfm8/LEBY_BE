package com.example.be.dto.response.question;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.question.EQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phản hồi chi tiết của một câu hỏi đơn hoặc chùm câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailResponse {

    private Long id;
    private String name;
    private EQuestionType type;
    private boolean isGroup;
    private Long partId;
    private String partName;
    private ESection section;
    private Integer difficulty;
    private String descriptions;

    /**
     * Dành cho chùm câu hỏi: Lời hướng dẫn chung.
     */
    private String instruction;

    /**
     * Dành cho chùm câu hỏi: Đoạn văn đọc hiểu (Part 6, 7).
     */
    private String passage;

    /**
     * Dành cho câu hỏi đơn: Nội dung câu hỏi.
     */
    private String text;

    /**
     * Dành cho câu hỏi đơn: Các lựa chọn A, B, C, D.
     */
    private List<QuestionOptionResponse> options;

    /**
     * Dành cho câu hỏi đơn: Nhãn đáp án đúng (A, B, C hoặc D).
     */
    private String correctKey;

    /**
     * Lời giải thích chi tiết.
     */
    private QuestionExplanationResponse explanation;

    /**
     * Danh sách tệp truyền thông đính kèm.
     */
    private List<QuestionMediaResponse> medias;

    /**
     * Danh sách kỹ năng năng lực.
     */
    private List<QuestionAbilityResponse> abilities;

    /**
     * Danh sách các câu hỏi con nếu đây là chùm câu hỏi.
     */
    private List<QuestionGroupItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

