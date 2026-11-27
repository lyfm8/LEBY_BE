package com.example.be.dto.request.mockexam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu lưu nháp tức thì các đáp án đã chọn và cờ đánh dấu trong phòng thi thử ETS 200 câu (UC17 - BR12).
 */
@Getter
@Setter
@NoArgsConstructor
public class SaveMockExamDraftRequest {

    /** ID lượt làm bài thi thử đang diễn ra. */
    @NotNull(message = "attemptId không được để trống")
    private Long attemptId;

    /** Danh sách câu trả lời/cờ cần đồng bộ nháp lên máy chủ (tối đa 250 phần tử chống DoS). */
    @NotNull(message = "Danh sách đáp án nháp không được để trống")
    @Size(max = 250, message = "Danh sách đáp án nháp không được vượt quá 250 câu")
    private List<@Valid DraftAnswerItem> answers;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DraftAnswerItem {

        /** ID câu hỏi trong đề thi thử. */
        @NotNull(message = "questionId không được để trống")
        private Long questionId;

        /** Ký hiệu đáp án chọn (A/B/C/D hoặc rỗng nếu bỏ chọn). */
        @Pattern(regexp = "^([A-Da-d])?$", message = "Đáp án lựa chọn chỉ chấp nhận A, B, C hoặc D")
        private String selectedOption;

        /** Cờ đánh dấu câu hỏi cần xem lại. */
        private Boolean isFlagged;
    }
}
