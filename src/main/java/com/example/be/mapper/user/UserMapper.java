package com.example.be.mapper.user;

import com.example.be.dto.response.user.AdminUserDetailResponse;
import com.example.be.dto.response.user.AdminUserDiagnosticHistoryItem;
import com.example.be.dto.response.user.AdminUserLearningPathItemDto;
import com.example.be.dto.response.user.AdminUserLearningPathResponse;
import com.example.be.dto.response.user.AdminUserMockTestHistoryItem;
import com.example.be.dto.response.user.AdminUserModuleTestHistoryItem;
import com.example.be.dto.response.user.UserActiveExamStatusResponse;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELessonStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper chuyên biệt chuyển đổi Entity User và dữ liệu học tập sang các DTO Quản trị viên (UC A02).
 */
@Component
public class UserMapper {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String AIM_PREFIX = "AIM ";

    /**
     * Chuyển đổi Entity {@link User} sang {@link UserResponse}.
     *
     * @param user Thực thể người dùng (có thể đã nạp sẵn TargetProfile qua EntityGraph)
     * @return UserResponse hoặc null nếu user đầu vào null
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        TargetProfile target = user.getTargetProfile();
        Integer aimScore = target != null ? target.getTargetTotalScore() : null;
        boolean isActive = Boolean.TRUE.equals(user.getIsActive());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(isActive ? STATUS_ACTIVE : STATUS_INACTIVE)
                .isActive(isActive)
                .targetProfileId(target != null ? target.getId() : null)
                .aimScore(aimScore)
                .aimTarget(aimScore != null ? AIM_PREFIX + aimScore : null)
                .learnerType(user.getLearnerType())
                .dob(user.getDob())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Chuyển đổi một lượt thi chẩn đoán sang DTO lịch sử hiển thị ở trang chi tiết.
     */
    public AdminUserDiagnosticHistoryItem toDiagnosticHistoryItem(
            DiagnosticAttempt attempt,
            Integer correctAnswers,
            Integer totalQuestions,
            Integer predictedScore
    ) {
        String testTitle = attempt.getDiagnosticTest() != null
                ? attempt.getDiagnosticTest().getTitle()
                : "Bài thi chẩn đoán";
        Integer targetScore = attempt.getTargetProfile() != null
                ? attempt.getTargetProfile().getTargetTotalScore()
                : null;

        return AdminUserDiagnosticHistoryItem.builder()
                .attemptId(attempt.getId())
                .testTitle(testTitle)
                .targetScore(targetScore)
                .status(attempt.getStatus() != null ? attempt.getStatus().name() : null)
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .correctAnswers(correctAnswers)
                .totalQuestions(totalQuestions)
                .predictedScore(predictedScore)
                .build();
    }

    /**
     * Chuyển đổi một chặng học {@link LearningPathItem} sang DTO hiển thị giống giao diện Student.
     */
    public AdminUserLearningPathItemDto toLearningPathItemDto(LearningPathItem item, int totalLessons) {
        Module module = item.getModule();
        int progressPercent = item.getProgressPercent() != null ? item.getProgressPercent() : 0;
        int completedLessons = totalLessons > 0
                ? (int) Math.round((progressPercent / 100.0) * totalLessons)
                : 0;
        // Bảo đảm số bài đã hoàn thành không vượt tổng bài học thực tế của mô-đun
        completedLessons = Math.max(0, Math.min(totalLessons, completedLessons));

        String partName = (module != null && module.getPart() != null)
                ? module.getPart().getName()
                : null;
        String section = (module != null && module.getPart() != null && module.getPart().getSections() != null)
                ? module.getPart().getSections().name()
                : null;

        return AdminUserLearningPathItemDto.builder()
                .id(item.getId())
                .orderNo(item.getOrderNo())
                .moduleId(module != null ? module.getId() : null)
                .moduleTitle(module != null ? module.getTitle() : "Mô-đun học tập")
                .moduleDescription(module != null ? module.getDescriptions() : null)
                .partName(partName)
                .section(section)
                .abilityName(item.getAbility() != null ? item.getAbility().getName() : null)
                .reason(item.getReason())
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .statusLabel(resolveLessonStatusLabel(item.getStatus()))
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercent(progressPercent)
                .isCorrect(item.getIsCorrect())
                .build();
    }

    /**
     * Chuyển đổi thực thể {@link LearningPath} và danh sách chặng đã map sang DTO tổng quan lộ trình.
     */
    public AdminUserLearningPathResponse toLearningPathResponse(
            LearningPath path,
            List<AdminUserLearningPathItemDto> itemDtos
    ) {
        if (path == null) {
            return null;
        }
        int totalModules = itemDtos.size();
        int completedModules = (int) itemDtos.stream()
                .filter(i -> ELessonStatus.COMPLETED.name().equals(i.getStatus()))
                .count();
        int progressPercent = totalModules > 0
                ? Math.round((completedModules * 100.0f) / totalModules)
                : 0;

        return AdminUserLearningPathResponse.builder()
                .learningPathId(path.getId())
                .version(path.getVersion())
                .status(path.getStatus() != null ? path.getStatus().name() : null)
                .targetScore(path.getTargetProfile() != null ? path.getTargetProfile().getTargetTotalScore() : null)
                .progressPercent(progressPercent)
                .totalModules(totalModules)
                .completedModules(completedModules)
                .createdAt(path.getCreatedAt())
                .items(itemDtos)
                .build();
    }

    /**
     * Chuyển đổi lượt thi vượt ải mô-đun {@link ModuleTestAttempt} sang DTO lịch sử.
     */
    public AdminUserModuleTestHistoryItem toModuleTestHistoryItem(ModuleTestAttempt attempt, Integer passScore) {
        Module module = attempt.getModule();
        String partName = (module != null && module.getPart() != null)
                ? module.getPart().getName()
                : null;

        return AdminUserModuleTestHistoryItem.builder()
                .attemptId(attempt.getId())
                .moduleId(module != null ? module.getId() : null)
                .moduleTitle(module != null ? module.getTitle() : "Mô-đun học tập")
                .partName(partName)
                .score(attempt.getScore())
                .passScore(passScore)
                .result(attempt.getResult() != null ? attempt.getResult().name() : null)
                .isSubmitted(Boolean.TRUE.equals(attempt.getIsSubmitted()))
                .submittedAt(attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : attempt.getCreatedAt())
                .build();
    }

    /**
     * Đóng gói toàn bộ dữ liệu chi tiết người dùng vào {@link AdminUserDetailResponse}.
     */
    public AdminUserDetailResponse toDetailResponse(
            User user,
            UserActiveExamStatusResponse activeExamStatus,
            List<AdminUserDiagnosticHistoryItem> diagnosticHistory,
            AdminUserLearningPathResponse learningPath,
            List<AdminUserModuleTestHistoryItem> moduleTestHistory,
            List<AdminUserMockTestHistoryItem> mockTestHistory
    ) {
        return AdminUserDetailResponse.builder()
                .profile(toResponse(user))
                .activeExamStatus(activeExamStatus)
                .diagnosticHistory(diagnosticHistory)
                .learningPath(learningPath)
                .moduleTestHistory(moduleTestHistory)
                .mockTestHistory(mockTestHistory)
                .build();
    }

    private String resolveLessonStatusLabel(ELessonStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case COMPLETED -> "Hoàn thành";
            case IN_PROGRESS -> "Đang học";
            case UNLOCKED -> "Đã mở khóa";
            case LOCKED -> "Đang khóa";
        };
    }
}
