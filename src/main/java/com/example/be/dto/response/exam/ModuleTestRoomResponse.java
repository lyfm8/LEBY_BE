package com.example.be.dto.response.exam;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ dữ liệu phòng thi vượt ải mô-đun trả về cho giao diện làm bài (UC13).
 */
@Getter
@Builder
public class ModuleTestRoomResponse {

    private Long attemptId;
    private Long moduleId;
    private String moduleTitle;
    private String partName;
    private String section;
    private String topic;
    private Integer passScore;
    private Integer targetScore;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private Long remainingSeconds;
    private Integer totalQuestions;
    private List<ModuleTestRoomQuestionResponse> questions;
    private List<ModuleTestDraftAnswerResponse> draftAnswers;
}
