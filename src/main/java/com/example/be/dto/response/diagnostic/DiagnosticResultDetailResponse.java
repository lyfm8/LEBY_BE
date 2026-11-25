package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Báo cáo kết quả chẩn đoán chi tiết toàn diện phục vụ màn hình UC07.
 */
@Getter
@Builder
public class DiagnosticResultDetailResponse {
    /** ID lượt làm bài chẩn đoán. */
    private Long attemptId;
    /** Tiêu đề bài thi chẩn đoán. */
    private String testTitle;
    /** Thời điểm hoàn thành bài thi. */
    private LocalDateTime completedAt;
    /** ID lộ trình học thích ứng gắn với lượt thi này (UC09). */
    private Long learningPathId;
    /** Điểm TOEIC dự đoán quy đổi thang 990 (chia hết cho 5). */
    private Integer predictedScore;
    /** Điểm mục tiêu AIM của học viên. */
    private Integer targetScore;
    /** Tổng số câu hỏi trong đề thi. */
    private Integer totalQuestions;
    /** Tổng số câu học viên trả lời đúng. */
    private Integer correctAnswers;
    /** Thời gian làm bài thực tế (giây). */
    private Integer durationSeconds;
    /** Điểm và tỷ lệ theo 2 kỹ năng Listening & Reading. */
    private SectionScoresResponse sectionScores;
    /** Kết quả chi tiết 7 Part TOEIC kèm các Ability con. */
    private List<PartResultDetailResponse> partResults;
    /** Dữ liệu 7 chiều vẽ Biểu đồ Radar năng lực. */
    private List<AbilityRadarResponse> abilitiesRadar;
    /** Danh sách các năng lực (Ability) cần tập trung bồi dưỡng (UC07 bước 5). */
    private List<PartAbilityBreakdownResponse> weakAbilities;
    /** Danh sách các phần thi được miễn học theo Tầng 1 (UC07 bước 5). */
    private List<String> passedParts;
    /** Khuyến nghị sư phạm tổng hợp theo AIM. */
    private DiagnosticAiRoadmapSummaryResponse aiRoadmapSummary;
}
