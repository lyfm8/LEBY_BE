package com.example.be.dto.request.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload lưu nháp tức thì đáp án và cờ đánh dấu của học viên trong phòng thi vượt ải (UC13 Luồng 7).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveModuleTestDraftRequest {

    @NotNull(message = "Mã lượt thi không được để trống")
    private Long attemptId;

    @NotNull(message = "Danh sách đáp án nháp không được để trống")
    @Size(min = 1, max = 200, message = "Số lượng đáp án nháp không vượt quá 200 câu")
    @Valid
    private List<DraftAnswerItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftAnswerItem {

        @NotNull(message = "Mã câu hỏi không được để trống")
        private Long questionId;

        @Pattern(regexp = "^[A-Da-d]$", message = "Phương án chọn chỉ được là A, B, C hoặc D")
        private String selectedOption;

        @NotNull(message = "Trạng thái đánh dấu không được để trống")
        private Boolean isFlagged;
    }
}
