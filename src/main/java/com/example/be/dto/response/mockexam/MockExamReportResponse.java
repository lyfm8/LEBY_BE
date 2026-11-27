package com.example.be.dto.response.mockexam;

import com.example.be.dto.response.diagnostic.AbilityRadarResponse;
import com.example.be.dto.response.diagnostic.PartAbilityBreakdownResponse;
import com.example.be.enums.diagnostic.EExamTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Báo cáo kết quả thi thử TOEIC 200 câu và Báo cáo nghiệm thu đầu ra Trước vs Sau (UC18).
 * - Đề PRACTICE: Chỉ trả điểm Listening/Reading/990 và thống kê cơ bản 7 Part (beforeRadar/afterRadar = null).
 * - Đề OUTPUT: Trả đầy đủ Radar Trước vs Sau (BR15), đối sánh 7 Part, phân tích Ability, điểm yếu & khuyến nghị.
 */
@Getter
@Builder
public class MockExamReportResponse {

    private Long attemptId;
    private Long testId;
    private String testTitle;
    private EExamTag examTag;
    private LocalDateTime completedAt;
    private Integer durationSeconds;

    private Integer listeningScore;
    private Integer readingScore;
    private Integer totalToeicScore;

    private Integer listeningCorrect;
    private Integer listeningTotal;
    private Integer listeningPercent;
    private Integer readingCorrect;
    private Integer readingTotal;
    private Integer readingPercent;

    private Integer totalCorrect;
    private Integer totalQuestions;

    private Integer targetScore;
    private Boolean isAimAchieved;
    private Boolean canViewReview;
    private Integer scoreDifference;

    /** Đường năng lực chẩn đoán đầu vào (màu xám trên Radar UC18 — null với đề PRACTICE). */
    private List<AbilityRadarResponse> beforeRadar;
    /** Đường năng lực đầu ra từ bài thi thử (màu xanh dương trên Radar UC18 — null với đề PRACTICE). */
    private List<AbilityRadarResponse> afterRadar;

    private List<MockExamPartComparisonItem> partResults;
    private List<PartAbilityBreakdownResponse> remainingWeakAbilities;
    private List<String> recommendations;

    private String summaryHeadline;
    private String summaryMessage;
}
