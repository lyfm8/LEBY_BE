package com.example.be.dto.response.mockexam;

import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ dữ liệu đề thi 200 câu, thời gian và bản nháp cho Phòng thi thử TOEIC (UC17).
 */
@Getter
@Builder
public class MockExamRoomResponse {

    private Long attemptId;
    private Long testId;
    private Long targetProfileId;
    private String title;
    private String description;
    private EExamTag examTag;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private Long remainingSeconds;
    private Integer totalQuestions;
    private List<DiagnosticQuestionResponse> questions;
    private List<MockExamDraftAnswerResponse> draftAnswers;
}
