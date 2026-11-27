package com.example.be.dto.request.mockexam;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu khởi tạo hoặc tiếp tục lượt thi thử TOEIC chuẩn ETS 200 câu (UC17).
 */
@Getter
@Setter
@NoArgsConstructor
public class StartMockExamRequest {

    /** ID đề thi thử (ETestType.FULL_MOCK_TEST) học viên chọn làm bài. */
    @NotNull(message = "diagnosticTestId không được để trống")
    private Long diagnosticTestId;

    /** ID mục tiêu điểm AIM áp dụng cho phiên thi (tùy chọn, mặc định lấy theo user). */
    private Long targetProfileId;
}
