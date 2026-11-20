package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê tăng trưởng người dùng mới và số lượt nộp bài test theo tháng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersByMonthDto {
    private String month;
    private long newUsersCount;
    private long testSubmissionsCount;
}
