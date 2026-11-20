package com.example.be.service.dashboard;

import com.example.be.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.be.dto.response.dashboard.DashboardStatsDto;
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
import com.example.be.service.dashboard.implement.AdminDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModuleTestAttemptRepository moduleTestAttemptRepository;

    @Mock
    private DiagnosticAttemptRepository diagnosticAttemptRepository;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private ModuleRepository moduleRepository;

    private AdminDashboardMapper adminDashboardMapper;
    private AdminDashboardServiceImpl adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardMapper = new AdminDashboardMapper();
        adminDashboardService = new AdminDashboardServiceImpl(
                userRepository,
                moduleTestAttemptRepository,
                diagnosticAttemptRepository,
                learningPathRepository,
                moduleRepository,
                adminDashboardMapper
        );
    }

    @Test
    @DisplayName("Lấy dữ liệu thống kê tổng quan (all-time) thành công")
    void testGetDashboardSummary_All_Success() {
        // Arrange
        when(userRepository.countByRole(ERole.STUDENT)).thenReturn(100L);
        when(userRepository.countByRoleAndCreatedAtBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);
        when(userRepository.countActiveStudentsSince(eq(ERole.STUDENT), any(LocalDateTime.class))).thenReturn(45L);
        when(userRepository.countActiveStudentsBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(30L);

        when(learningPathRepository.countByStatus(ELearningPathStatus.COMPLETED)).thenReturn(35L);
        when(learningPathRepository.count()).thenReturn(50L);

        when(moduleTestAttemptRepository.countByIsSubmittedTrue()).thenReturn(200L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrueAndResult(ETestResult.PASS)).thenReturn(160L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrueAndSubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(25L);

        when(moduleRepository.count()).thenReturn(14L);

        when(userRepository.countUsersByAimTarget(ERole.STUDENT)).thenReturn(Collections.singletonList(
                new Object[]{650, 40L}
        ));
        when(moduleTestAttemptRepository.findTopDifficultModules(eq(ETestResult.FAIL), any(Pageable.class))).thenReturn(Collections.singletonList(
                new Object[]{1L, "Module 1", "Part 1", 20L, 8L}
        ));
        when(moduleTestAttemptRepository.findTopRecentAttempts(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findTopRecentAttempts(eq(EAttemptStatus.SUBMITTED), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(userRepository.findTop5ByRoleOrderByCreatedAtDesc(ERole.STUDENT)).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardSummaryResponse response = adminDashboardService.getDashboardSummary("all");

        // Assert
        assertThat(response).isNotNull();
        DashboardStatsDto stats = response.getStats();
        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getActiveUsersThisMonth()).isEqualTo(45L);
        assertThat(stats.getLearningPathCompletionRate()).isEqualTo(70.0); // 35 / 50 * 100
        assertThat(stats.getModuleTestPassRate()).isEqualTo(80.0); // 160 / 200 * 100
        assertThat(stats.getTotalModules()).isEqualTo(14L);

        assertThat(response.getUsersByMonth()).hasSize(6);
        assertThat(response.getAimDistribution()).hasSize(1);
        assertThat(response.getAimDistribution().get(0).getAimName()).isEqualTo("AIM 650");
        assertThat(response.getTopDifficultModules()).hasSize(1);
        assertThat(response.getTopDifficultModules().get(0).getFailPercent()).isEqualTo(40.0); // 8 / 20 * 100
    }

    @Test
    @DisplayName("Xử lý an toàn khi hệ thống mới tinh không có dữ liệu (Triệt tiêu lỗi chia cho 0)")
    void testZeroDivisionSafety_Success() {
        // Arrange: Tất cả các bảng rỗng (count = 0)
        when(userRepository.countByRole(ERole.STUDENT)).thenReturn(0L);
        when(userRepository.countByRoleAndCreatedAtBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countActiveStudentsSince(eq(ERole.STUDENT), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepository.countActiveStudentsBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(learningPathRepository.countByStatus(ELearningPathStatus.COMPLETED)).thenReturn(0L);
        when(learningPathRepository.count()).thenReturn(0L);

        when(moduleTestAttemptRepository.countByIsSubmittedTrue()).thenReturn(0L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrueAndResult(ETestResult.PASS)).thenReturn(0L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrueAndSubmittedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(moduleRepository.count()).thenReturn(0L);
        when(userRepository.countUsersByAimTarget(ERole.STUDENT)).thenReturn(Collections.emptyList());
        when(moduleTestAttemptRepository.findTopDifficultModules(eq(ETestResult.FAIL), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(moduleTestAttemptRepository.findTopRecentAttempts(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findTopRecentAttempts(eq(EAttemptStatus.SUBMITTED), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(userRepository.findTop5ByRoleOrderByCreatedAtDesc(ERole.STUDENT)).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardSummaryResponse response = adminDashboardService.getDashboardSummary("all");

        // Assert: Không crash, tỷ lệ trả về 0.0 an toàn
        assertThat(response).isNotNull();
        assertThat(response.getStats().getLearningPathCompletionRate()).isEqualTo(0.0);
        assertThat(response.getStats().getModuleTestPassRate()).isEqualTo(0.0);
        assertThat(response.getStats().getTotalUsersGrowthPercent()).isEqualTo(0.0);
        assertThat(response.getStats().getActiveUsersGrowthPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Kiểm tra tính toán tỷ lệ % và định dạng thời gian trong Mapper")
    void testMapperFunctions() {
        assertThat(adminDashboardMapper.calculatePercentage(0, 0)).isEqualTo(0.0);
        assertThat(adminDashboardMapper.calculatePercentage(1, 3)).isEqualTo(33.3);
        assertThat(adminDashboardMapper.calculateGrowth(120, 100)).isEqualTo(20.0);
        assertThat(adminDashboardMapper.calculateGrowth(50, 0)).isEqualTo(100.0);

        LocalDateTime now = LocalDateTime.now();
        assertThat(adminDashboardMapper.formatTimeAgo(now.minusSeconds(10), now)).isEqualTo("Vừa xong");
        assertThat(adminDashboardMapper.formatTimeAgo(now.minusMinutes(15), now)).isEqualTo("15 phút trước");
        assertThat(adminDashboardMapper.formatTimeAgo(now.minusHours(3), now)).isEqualTo("3 giờ trước");
        assertThat(adminDashboardMapper.formatTimeAgo(now.minusDays(5), now)).isEqualTo("5 ngày trước");
    }

    @Test
    @DisplayName("Loại bỏ mô-đun có số lượt trượt bằng 0 khỏi danh sách Top mô-đun khó")
    void testFindTopDifficultModules_ExcludeZeroFails() {
        when(userRepository.countByRole(ERole.STUDENT)).thenReturn(10L);
        when(userRepository.countByRoleAndCreatedAtBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(userRepository.countActiveStudentsSince(eq(ERole.STUDENT), any(LocalDateTime.class))).thenReturn(1L);
        when(userRepository.countActiveStudentsBetween(eq(ERole.STUDENT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(learningPathRepository.countByStatus(ELearningPathStatus.COMPLETED)).thenReturn(1L);
        when(learningPathRepository.count()).thenReturn(1L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrue()).thenReturn(1L);
        when(moduleTestAttemptRepository.countByIsSubmittedTrueAndResult(ETestResult.PASS)).thenReturn(1L);
        when(moduleRepository.count()).thenReturn(1L);
        when(userRepository.countUsersByAimTarget(ERole.STUDENT)).thenReturn(Collections.emptyList());

        // Mô-đun có 1 lượt thi nhưng 0 lượt trượt (failCount = 0L)
        when(moduleTestAttemptRepository.findTopDifficultModules(eq(ETestResult.FAIL), any(Pageable.class))).thenReturn(Collections.singletonList(
                new Object[]{2L, "Wh-Questions", "Part 2", 1L, 0L}
        ));
        when(moduleTestAttemptRepository.findTopRecentAttempts(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findTopRecentAttempts(eq(EAttemptStatus.SUBMITTED), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(userRepository.findTop5ByRoleOrderByCreatedAtDesc(ERole.STUDENT)).thenReturn(Collections.emptyList());

        AdminDashboardSummaryResponse response = adminDashboardService.getDashboardSummary("all");

        assertThat(response.getTopDifficultModules()).isEmpty();
    }
}
