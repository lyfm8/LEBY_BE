package com.example.be.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chứa các chỉ số KPI học tập cốt lõi tổng hợp từ dữ liệu thật (UC08 Bước 3).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardStatsDto {

    /** Tỷ lệ hoàn thành toàn lộ trình (0 - 100), luôn xử lý an toàn khi totalModules = 0. */
    private int roadmapProgressPercent;

    private int completedModules;
    private int totalModules;
    private int completedLessons;
    private int totalLessons;

    /** Số ngày học liên tục tính theo múi giờ Asia/Ho_Chi_Minh. */
    private int streakDays;

    /** Đánh dấu hôm nay học viên đã có ít nhất 1 hoạt động học/thi hay chưa. */
    @JsonProperty("isStudiedToday")
    private Boolean isStudiedToday;

    /** Tổng số phút học tích lũy từ trước đến nay. */
    private int totalStudyMinutes;

    /** Tổng số phút học trong 7 ngày gần nhất. */
    private int weeklyStudyMinutes;

    /** Số ngày có hoạt động học trong 7 ngày gần nhất (0 - 7). */
    private int weeklyActiveDays;

    /** Điểm TOEIC dự đoán hiện tại theo thang 990 (null nếu chưa thi chẩn đoán). */
    private Integer predictedScore;

    /** Mốc điểm TOEIC mục tiêu AIM hiện tại (null nếu chưa chọn). */
    private Integer targetScore;

    /** Cờ xác định học viên PASS 100% 7 Part ngay ở bài thi chẩn đoán đầu vào. */
    @JsonProperty("isPassedAllParts")
    private Boolean isPassedAllParts;

    /** Cờ xác định học viên đã đủ điều kiện mở khóa bài thi thử TOEIC 200 câu đầu ra (UC17). */
    @JsonProperty("canTakeFullMockTest")
    private Boolean canTakeFullMockTest;
}
