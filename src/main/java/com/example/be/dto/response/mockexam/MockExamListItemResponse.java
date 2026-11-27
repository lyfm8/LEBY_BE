package com.example.be.dto.response.mockexam;

import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin một đề thi thử TOEIC 200 câu trong danh sách phòng thi (UC17).
 */
@Getter
@Builder
public class MockExamListItemResponse {

    private Long id;
    private String title;
    private String description;
    private EExamTag examTag;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private Boolean isLocked;
    private String lockReason;
    private Boolean hasInProgressAttempt;
    private Long inProgressAttemptId;
    private Integer attemptCount;
    private Integer bestScore;
}
