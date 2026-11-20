package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 4 chỉ số KPI cốt lõi và 2 chỉ số phụ trên Admin Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalUsers;
    private double totalUsersGrowthPercent;
    private long activeUsersThisMonth;
    private double activeUsersGrowthPercent;
    private double learningPathCompletionRate;
    private double moduleTestPassRate;
    private long totalTestsCompleted;
    private long totalModules;
}
