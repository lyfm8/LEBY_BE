package com.example.be.dto.response.mockexam;

import lombok.Builder;
import lombok.Getter;

/**
 * Đáp án nháp đã lưu trên máy chủ của một câu hỏi trong phiên thi thử (BR12).
 */
@Getter
@Builder
public class MockExamDraftAnswerResponse {

    private Long questionId;
    private Integer orderNo;
    private String selectedOption;
    private Boolean isFlagged;
}
