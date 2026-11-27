package com.example.be.dto.response.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO biểu diễn câu hỏi ôn tập củng cố được gắn kèm bài giảng Video (UC11).
 * Hỗ trợ đầy đủ cả câu hỏi đơn lập (Part 1, 2, 5) và câu hỏi chùm có ngữ cảnh cha (Part 3, 4, 6, 7).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLessonReviewQuestionResponse {

    private Long id;
    private Long questionId;
    private Integer orderNo;
    private Integer points;
    private String questionText;
    private String imageUrl;
    private String audioUrl;
    private String parentContextText;
    private String parentImageUrl;
    private String parentAudioUrl;
    private List<ReviewOptionItem> options;
    private String correctAnswer;
    private ReviewExplanationItem explanation;

    /**
     * Một phương án lựa chọn A/B/C/D của câu hỏi ôn tập.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewOptionItem {
        private String key;
        private String text;
    }

    /**
     * Một mục từ vựng quan trọng trong phần giải thích đáp án.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewVocabularyItem {
        private String word;
        private String type;
        private String meaning;
    }

    /**
     * Cấu trúc giải thích chi tiết đáp án phục vụ học viên tự ôn tập tức thì.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewExplanationItem {
        private String transcript;
        private String translation;
        private String clue;
        private String analysis;
        private List<ReviewVocabularyItem> vocabulary;
    }
}
