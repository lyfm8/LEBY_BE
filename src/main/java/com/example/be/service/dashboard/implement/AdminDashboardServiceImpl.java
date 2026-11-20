package com.example.be.service.dashboard.implement;

import com.example.be.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.be.dto.response.dashboard.AimDistributionDto;
import com.example.be.dto.response.dashboard.DashboardStatsDto;
import com.example.be.dto.response.dashboard.RecentActivityDto;
import com.example.be.dto.response.dashboard.TopDifficultModuleDto;
import com.example.be.dto.response.dashboard.UsersByMonthDto;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.enums.user.ERole;
import com.example.be.mapper.dashboard.AdminDashboardMapper;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Triển khai nghiệp vụ tổng hợp thống kê phân tích số liệu cho Quản trị viên (Admin Dashboard A01).
 * 100% Phép toán gom nhóm được đẩy xuống Database Engine thông qua JPA Aggregations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final ModuleTestAttemptRepository moduleTestAttemptRepository;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final LearningPathRepository learningPathRepository;
    private final ModuleRepository moduleRepository;
    private final AdminDashboardMapper adminDashboardMapper;

    /**
     * Tổng hợp toàn bộ số liệu thống kê phân tích số liệu hệ thống cho Admin Dashboard.
     *
     * @param timeRange Khoảng thời gian lọc ("all", "this_month", "30d", "7d")
     * @return AdminDashboardSummaryResponse DTO chứa dữ liệu tổng quan, tăng trưởng và hoạt động
     */
    @Override
    public AdminDashboardSummaryResponse getDashboardSummary(String timeRange) {
        LocalDateTime now = LocalDateTime.now();

        // Bước 1: Xác định mốc thời gian lọc dữ liệu theo tham số query
        LocalDateTime filterStart = resolveFilterStartTime(timeRange, now);

        // Bước 2: Tổng hợp 4 chỉ số KPI cốt lõi và 2 chỉ số quy mô
        DashboardStatsDto stats = calculateStats(filterStart, now);

        // Bước 3: Thống kê tăng trưởng 6 tháng gần nhất (Học viên mới & Lượt nộp bài thi)
        List<UsersByMonthDto> usersByMonth = calculateUsersByMonth(now);

        // Bước 4: Thống kê tỷ lệ phân bố theo mốc điểm mục tiêu TOEIC (AIM)
        List<AimDistributionDto> aimDistribution = calculateAimDistribution(stats.getTotalUsers());

        // Bước 5: Truy vấn Top 5 mô-đun có tỷ lệ thi trượt cao nhất (O(N) Pushdown)
        List<TopDifficultModuleDto> topDifficultModules = findTopDifficultModules();

        // Bước 6: Tập hợp nhật ký các hoạt động mới nhất từ đa nguồn
        List<RecentActivityDto> recentActivities = getRecentActivities(now);

        return AdminDashboardSummaryResponse.builder()
                .stats(stats)
                .usersByMonth(usersByMonth)
                .aimDistribution(aimDistribution)
                .topDifficultModules(topDifficultModules)
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * Xác định thời điểm bắt đầu lọc dữ liệu theo tham số.
     */
    private LocalDateTime resolveFilterStartTime(String timeRange, LocalDateTime now) {
        if (timeRange == null || "all".equalsIgnoreCase(timeRange)) {
            return null;
        }
        if ("7d".equalsIgnoreCase(timeRange)) {
            return now.minusDays(7);
        }
        if ("30d".equalsIgnoreCase(timeRange)) {
            return now.minusDays(30);
        }
        if ("this_month".equalsIgnoreCase(timeRange)) {
            return now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        return null;
    }

    /**
     * Tính toán các chỉ số KPI: Tăng trưởng học viên, Học viên Active, Tỷ lệ đỗ, Tỷ lệ hoàn thành lộ trình.
     */
    private DashboardStatsDto calculateStats(LocalDateTime filterStart, LocalDateTime now) {
        // Tổng học viên toàn hệ thống (All-time Platform Size)
        long totalUsers = userRepository.countByRole(ERole.STUDENT);

        // Tính tốc độ tăng trưởng học viên mới tháng này vs tháng trước
        LocalDateTime startThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startPrevMonth = startThisMonth.minusMonths(1);

        long thisMonthNewUsers = userRepository.countByRoleAndCreatedAtBetween(ERole.STUDENT, startThisMonth, now);
        long prevMonthNewUsers = userRepository.countByRoleAndCreatedAtBetween(ERole.STUDENT, startPrevMonth, startThisMonth);
        double totalUsersGrowthPercent = adminDashboardMapper.calculateGrowth(thisMonthNewUsers, prevMonthNewUsers);

        // Học viên Active (Có phát sinh nộp bài thi vượt ải hoặc thi chẩn đoán)
        long activeThisMonth = userRepository.countActiveStudentsSince(ERole.STUDENT, startThisMonth);
        long activePrevMonth = userRepository.countActiveStudentsBetween(ERole.STUDENT, startPrevMonth, startThisMonth);
        double activeUsersGrowthPercent = adminDashboardMapper.calculateGrowth(activeThisMonth, activePrevMonth);

        // Tỷ lệ hoàn thành lộ trình học tập thích ứng
        long completedPaths = learningPathRepository.countByStatus(ELearningPathStatus.COMPLETED);
        long totalPaths = learningPathRepository.count();
        double learningPathCompletionRate = adminDashboardMapper.calculatePercentage(completedPaths, totalPaths);

        // Tỷ lệ thi đỗ bài thi vượt ải mô-đun và tổng lượt thi hoàn thành
        long totalTestsCompleted;
        long passTestsCount;
        if (filterStart != null) {
            totalTestsCompleted = moduleTestAttemptRepository.countByIsSubmittedTrueAndSubmittedAtBetween(filterStart, now);
            passTestsCount = moduleTestAttemptRepository.countByIsSubmittedTrueAndResultAndSubmittedAtBetween(ETestResult.PASS, filterStart, now);
        } else {
            totalTestsCompleted = moduleTestAttemptRepository.countByIsSubmittedTrue();
            passTestsCount = moduleTestAttemptRepository.countByIsSubmittedTrueAndResult(ETestResult.PASS);
        }
        double moduleTestPassRate = adminDashboardMapper.calculatePercentage(passTestsCount, totalTestsCompleted);

        // Tổng số lượng mô-đun học liệu trong hệ thống
        long totalModules = moduleRepository.count();

        return DashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalUsersGrowthPercent(totalUsersGrowthPercent)
                .activeUsersThisMonth(activeThisMonth)
                .activeUsersGrowthPercent(activeUsersGrowthPercent)
                .learningPathCompletionRate(learningPathCompletionRate)
                .moduleTestPassRate(moduleTestPassRate)
                .totalTestsCompleted(totalTestsCompleted)
                .totalModules(totalModules)
                .build();
    }

    /**
     * Thống kê số lượng học viên mới và số lượt nộp bài test theo từng tháng (6 tháng gần nhất).
     */
    private List<UsersByMonthDto> calculateUsersByMonth(LocalDateTime now) {
        List<UsersByMonthDto> result = new ArrayList<>();
        LocalDateTime startThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = startThisMonth.minusMonths(i);
            LocalDateTime monthEnd = (i == 0) ? now : monthStart.plusMonths(1);
            String monthLabel = String.format("Tháng %02d", monthStart.getMonthValue());

            long newUsers = userRepository.countByRoleAndCreatedAtBetween(ERole.STUDENT, monthStart, monthEnd);
            long testSubmissions = moduleTestAttemptRepository.countByIsSubmittedTrueAndSubmittedAtBetween(monthStart, monthEnd);

            result.add(UsersByMonthDto.builder()
                    .month(monthLabel)
                    .newUsersCount(newUsers)
                    .testSubmissionsCount(testSubmissions)
                    .build());
        }
        return result;
    }

    /**
     * Thống kê phân bố học viên theo từng mốc mục tiêu AIM.
     */
    private List<AimDistributionDto> calculateAimDistribution(long totalUsers) {
        List<AimDistributionDto> distribution = new ArrayList<>();
        List<Object[]> rawCounts = userRepository.countUsersByAimTarget(ERole.STUDENT);

        for (Object[] row : rawCounts) {
            if (row[0] != null) {
                int aimScore = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                double percent = adminDashboardMapper.calculatePercentage(count, totalUsers);

                distribution.add(AimDistributionDto.builder()
                        .aimName("AIM " + aimScore)
                        .count(count)
                        .percent(percent)
                        .build());
            }
        }
        return distribution;
    }

    /**
     * Lấy Top 5 mô-đun khó có tỷ lệ thi trượt cao nhất trực tiếp từ câu query JPA.
     * BẮT BUỘC: Chỉ nhận các mô-đun thực sự có học viên thi trượt (failCount > 0).
     */
    private List<TopDifficultModuleDto> findTopDifficultModules() {
        List<Object[]> rawModules = moduleTestAttemptRepository.findTopDifficultModules(ETestResult.FAIL, PageRequest.of(0, 5));
        List<TopDifficultModuleDto> list = new ArrayList<>();
        for (Object[] row : rawModules) {
            TopDifficultModuleDto dto = adminDashboardMapper.toTopDifficultModuleDto(row);
            if (dto.getFailCount() > 0) {
                list.add(dto);
            }
        }
        return list;
    }

    /**
     * Tập hợp 10 hoạt động gần nhất từ Module Test Attempt, Diagnostic Attempt và Đăng ký mới.
     */
    private List<RecentActivityDto> getRecentActivities(LocalDateTime now) {
        List<RecentActivityDto> combined = new ArrayList<>();

        // 1. Lượt thi vượt ải gần nhất
        List<ModuleTestAttempt> recentModuleTests = moduleTestAttemptRepository.findTopRecentAttempts(PageRequest.of(0, 5));
        for (ModuleTestAttempt attempt : recentModuleTests) {
            combined.add(adminDashboardMapper.toActivityDto(attempt, now));
        }

        // 2. Lượt thi chẩn đoán gần nhất
        List<DiagnosticAttempt> recentDiagnostics = diagnosticAttemptRepository.findTopRecentAttempts(EAttemptStatus.SUBMITTED, PageRequest.of(0, 5));
        for (DiagnosticAttempt attempt : recentDiagnostics) {
            combined.add(adminDashboardMapper.toActivityDto(attempt, now));
        }

        // 3. Học viên mới đăng ký gần nhất
        List<User> newStudents = userRepository.findTop5ByRoleOrderByCreatedAtDesc(ERole.STUDENT);
        for (User user : newStudents) {
            combined.add(adminDashboardMapper.toActivityDto(user, now));
        }

        // Sắp xếp giảm dần theo thời gian và lấy tối đa 10 sự kiện
        combined.sort(Comparator.comparing(RecentActivityDto::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        if (combined.size() > 10) {
            return combined.subList(0, 10);
        }
        return combined;
    }
}
