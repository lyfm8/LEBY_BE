package com.example.be.dto.response.content;

import com.example.be.dto.response.exam.ExamOptionResponse;
import com.example.be.enums.content.EMistakeSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO khởi tạo phiên làm lại nguyên bộ câu hỏi sai (UC15 Luồng 5a).
 * Bảo mật: Tuyệt đối không trả {@code correctAnswer} hay {@code explanation} trước khi học viên nộp bài.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeRetrySetResponse {

    private Integer totalAvailable;
    private Integer sessionSize;
    private List<RetryQuestionItem> questions;

    /**
     * Dữ liệu 1 câu hỏi trong phiên làm lại (đã ẩn đáp án đúng và lời giải).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryQuestionItem {
        private Long questionId;
        private Integer orderNo;
        private Long parentId;
        private Long partId;
        private Integer partNo;
        private String partName;
        private String section;
        private List<MistakeQuestionItemResponse.AbilityTag> abilities;
        private EMistakeSourceType sourceType;
        private String sourceLabel;
        private Integer wrongCount;

        private String title;
        private String instruction;
        private String passage;
        private String audioUrl;
        private String imageUrl;
        private Boolean isGroupQuestion;
        private List<ExamOptionResponse> options;
    }
}
