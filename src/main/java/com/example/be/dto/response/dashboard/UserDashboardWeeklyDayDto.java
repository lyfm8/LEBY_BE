package com.example.be.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO thống kê tiến độ học tập theo từng ngày trong biểu đồ 7 ngày gần nhất (UC08 Bước 6).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardWeeklyDayDto {

    /** Ngày dạng ISO (yyyy-MM-dd) theo múi giờ Asia/Ho_Chi_Minh. */
    private String date;

    /** Nhãn thứ ngắn gọn tiếng Việt (T2, T3, T4, T5, T6, T7, CN). */
    private String dayLabel;

    /** Tổng số phút học/thi ghi nhận trong ngày. */
    private int studyMinutes;

    /** Số bài giảng đã hoàn thành trong ngày. */
    private int completedLessons;

    /** Số lượt bài thi (chẩn đoán / vượt ải / thi thử) đã nộp trong ngày. */
    private int testsSubmitted;

    /** Cờ xác định cột này là ngày hôm nay. */
    @JsonProperty("isToday")
    private Boolean isToday;
}
