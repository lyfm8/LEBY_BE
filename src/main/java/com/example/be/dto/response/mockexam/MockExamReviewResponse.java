package com.example.be.dto.response.mockexam;

import com.example.be.dto.response.diagnostic.DiagnosticReviewQuestionResponse;
import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu phòng xem lại chi tiết từng câu hỏi bài thi thử TOEIC 200 câu đã nộp (BR14).
 */
@Getter
@Builder
public class MockExamReviewResponse {

    private Long attemptId;
    private Long testId;
    private String testTitle;
    private EExamTag examTag;
    private LocalDateTime completedAt;
    private Integer totalQuestions;
    private Integer correctCount;
    private List<DiagnosticReviewQuestionResponse> questions;
}
