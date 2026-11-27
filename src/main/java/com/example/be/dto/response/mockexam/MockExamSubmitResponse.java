package com.example.be.dto.response.mockexam;

import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả tóm tắt trả về ngay sau khi học viên nộp bài thi thử ETS 200 câu (UC17).
 */
@Getter
@Builder
public class MockExamSubmitResponse {

    private Long attemptId;
    private Long testId;
    private EExamTag examTag;
    private Integer listeningScore;
    private Integer readingScore;
    private Integer totalToeicScore;
    private Integer totalCorrect;
    private Integer totalQuestions;
    private Boolean isAimAchieved;
}
