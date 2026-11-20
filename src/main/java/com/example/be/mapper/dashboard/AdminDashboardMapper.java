package com.example.be.mapper.dashboard;

import com.example.be.dto.response.dashboard.RecentActivityDto;
import com.example.be.dto.response.dashboard.TopDifficultModuleDto;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.user.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Mapper Component chuyên biệt phục vụ chuyển đổi số liệu và Entity sang DTO cho Admin Dashboard.
 * Đảm bảo Zero-division handling và định dạng hiển thị thân thiện chuẩn Việt hóa.
 */
@Component
public class AdminDashboardMapper {

    /**
     * Tính tỷ lệ phần trăm an toàn, triệt tiêu hoàn toàn lỗi chia cho 0 khi mẫu số bằng 0.
     * Làm tròn 1 chữ số thập phân (ví dụ: 76.5%).
     */
    public double calculatePercentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 100.0 / denominator) * 10.0) / 10.0;
    }

    /**
     * Tính tốc độ tăng trưởng (%) giữa kỳ hiện tại và kỳ trước.
     */
    public double calculateGrowth(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - previous) * 100.0 / previous) * 10.0) / 10.0;
    }

    /**
     * Định dạng khoảng cách thời gian thân thiện (Human-friendly time ago).
     */
    public String formatTimeAgo(LocalDateTime timestamp, LocalDateTime now) {
        if (timestamp == null) {
            return "Vừa xong";
        }
        Duration duration = Duration.between(timestamp, now);
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "Vừa xong";
        }
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }
        long days = duration.toDays();
        if (days < 30) {
            return days + " ngày trước";
        }
        return timestamp.toLocalDate().toString();
    }

    /**
     * Chuyển đổi lượt thi vượt ải sang DTO nhật ký hoạt động.
     */
    public RecentActivityDto toActivityDto(ModuleTestAttempt attempt, LocalDateTime now) {
        String fullName = attempt.getUser() != null && attempt.getUser().getFullName() != null
                ? attempt.getUser().getFullName() : "Học viên";
        String avatar = attempt.getUser() != null ? attempt.getUser().getAvatar() : null;
        String aimTarget = "Chưa đặt AIM";
        if (attempt.getUser() != null && attempt.getUser().getTargetProfile() != null) {
            aimTarget = "AIM " + attempt.getUser().getTargetProfile().getTargetTotalScore();
        }
        String moduleTitle = attempt.getModule() != null ? attempt.getModule().getTitle() : "Mô-đun học";
        String badge = attempt.getResult() != null ? attempt.getResult().name() : "PASS";

        return RecentActivityDto.builder()
                .id(attempt.getId())
                .userId(attempt.getUser() != null ? attempt.getUser().getId() : null)
                .fullName(fullName)
                .avatar(avatar)
                .action("Vượt ải Module: " + moduleTitle)
                .aimTarget(aimTarget)
                .timeAgo(formatTimeAgo(attempt.getSubmittedAt(), now))
                .timestamp(attempt.getSubmittedAt())
                .resultBadge(badge)
                .score(attempt.getScore())
                .build();
    }

    /**
     * Chuyển đổi lượt thi chẩn đoán sang DTO nhật ký hoạt động.
     */
    public RecentActivityDto toActivityDto(DiagnosticAttempt attempt, LocalDateTime now) {
        String fullName = attempt.getUser() != null && attempt.getUser().getFullName() != null
                ? attempt.getUser().getFullName() : "Học viên";
        String avatar = attempt.getUser() != null ? attempt.getUser().getAvatar() : null;
        String testTitle = attempt.getDiagnosticTest() != null ? attempt.getDiagnosticTest().getTitle() : "Đề chẩn đoán";

        return RecentActivityDto.builder()
                .id(attempt.getId())
                .userId(attempt.getUser() != null ? attempt.getUser().getId() : null)
                .fullName(fullName)
                .avatar(avatar)
                .action("Nộp bài thi: " + testTitle)
                .aimTarget("Chẩn đoán")
                .timeAgo(formatTimeAgo(attempt.getCompletedAt(), now))
                .timestamp(attempt.getCompletedAt())
                .resultBadge("DIAGNOSTIC")
                .score(null)
                .build();
    }

    /**
     * Chuyển đổi đăng ký tài khoản sang DTO nhật ký hoạt động.
     */
    public RecentActivityDto toActivityDto(User user, LocalDateTime now) {
        String fullName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String aimTarget = user.getTargetProfile() != null ? "AIM " + user.getTargetProfile().getTargetTotalScore() : "Mới";

        return RecentActivityDto.builder()
                .id(user.getId())
                .userId(user.getId())
                .fullName(fullName)
                .avatar(user.getAvatar())
                .action("Đăng ký tài khoản học viên")
                .aimTarget(aimTarget)
                .timeAgo(formatTimeAgo(user.getCreatedAt(), now))
                .timestamp(user.getCreatedAt())
                .resultBadge("NEW")
                .score(null)
                .build();
    }

    /**
     * Chuyển đổi Object[] từ JPA query sang TopDifficultModuleDto.
     */
    public TopDifficultModuleDto toTopDifficultModuleDto(Object[] row) {
        Long moduleId = ((Number) row[0]).longValue();
        String moduleTitle = (String) row[1];
        String partName = row[2] != null ? (String) row[2] : "Part N/A";
        long totalAttempts = ((Number) row[3]).longValue();
        long failCount = ((Number) row[4]).longValue();
        double failPercent = calculatePercentage(failCount, totalAttempts);

        return TopDifficultModuleDto.builder()
                .moduleId(moduleId)
                .moduleTitle(moduleTitle)
                .partName(partName)
                .totalAttempts(totalAttempts)
                .failCount(failCount)
                .failPercent(failPercent)
                .build();
    }
}
