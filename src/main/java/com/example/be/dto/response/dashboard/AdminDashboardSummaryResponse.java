package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Phản hồi tổng hợp toàn diện cho Admin Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {
    private DashboardStatsDto stats;
    private List<UsersByMonthDto> usersByMonth;
    private List<AimDistributionDto> aimDistribution;
    private List<TopDifficultModuleDto> topDifficultModules;
    private List<RecentActivityDto> recentActivities;
}
