package com.example.be.dto.response.mockexam;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu tổng hợp cho khu vực "Thi thử TOEIC chuẩn ETS" (UC17).
 */
@Getter
@Builder
public class MockExamListResponse {

    private Boolean isOutputExamUnlocked;
    private String outputLockReason;
    private Long activeAttemptId;
    private Long activeTestId;
    private String activeTestTitle;
    private Integer userTargetScore;
    private List<MockExamListItemResponse> exams;
    private List<MockExamHistoryItemResponse> history;
}
