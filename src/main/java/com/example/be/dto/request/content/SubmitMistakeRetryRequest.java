package com.example.be.dto.request.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO nộp danh sách đáp án cho phiên làm lại bộ câu hỏi sai trong Sổ tay (UC15 Luồng 5a).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMistakeRetryRequest {

    /**
     * Danh sách câu trả lời học viên nộp trong phiên làm lại (giới hạn tối đa 50 câu chống Payload DoS).
     */
    @Valid
    @NotEmpty(message = "Danh sách câu trả lời không được để trống")
    @Size(max = 50, message = "Mỗi phiên làm lại chỉ chấp nhận tối đa 50 câu hỏi")
    private List<MistakeRetryAnswerItem> answers;

    /**
     * Một mục đáp án học viên chọn cho câu hỏi trong phiên làm lại.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakeRetryAnswerItem {

        @NotNull(message = "Mã câu hỏi không được để trống")
        @Positive(message = "Mã câu hỏi phải là số nguyên dương")
        private Long questionId;

        /**
         * Phương án học viên chọn (A, B, C hoặc D; có thể null nếu học viên bỏ trống câu đó).
         */
        @Pattern(
                regexp = "^[A-Da-d]$",
                message = "Đáp án lựa chọn chỉ chấp nhận A, B, C hoặc D"
        )
        private String selectedAnswer;
    }
}
