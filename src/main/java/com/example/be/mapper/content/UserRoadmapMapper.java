package com.example.be.mapper.content;

import com.example.be.dto.response.content.RoadmapModuleItemResponse;
import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;
import com.example.be.dto.response.content.UserModuleLessonResponse;
import com.example.be.dto.response.content.UserModuleTestInfoResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Mapper chuyên biệt chuyển đổi Entity sang DTO cho luồng Lộ trình học cá nhân (UC09)
 * và Chi tiết Module (UC10) của học viên.
 */
@Component
public class UserRoadmapMapper {

    private static final int FULL_PERCENT = 100;
    private static final int ZERO_PERCENT = 0;

    private static final String DEFAULT_PART_NAME = "Chuyên đề tổng hợp";
    private static final String STATUS_COMPLETED_LABEL = "Đã hoàn thành";
    private static final String STATUS_IN_PROGRESS_LABEL = "Đang học";
    private static final String STATUS_UNLOCKED_LABEL = "Sẵn sàng học";
    private static final String STATUS_LOCKED_LABEL = "Chưa mở khóa";
    private static final String STATUS_FREE_STUDY_LABEL = "Học tự do";

    /**
     * Chuyển đổi Entity LearningPath và danh sách chặng Module sang RoadmapResponse tổng quan.
     *
     * @param learningPath   Lộ trình học hiện hành của học viên
     * @param moduleItems    Danh sách chặng học đã được map kèm thống kê bài học
     * @param passedAllParts Cờ xác nhận học viên đã PASS 100% 7 Part ở bài chẩn đoán (Luồng 4a)
     * @return RoadmapResponse hoàn chỉnh cho giao diện Lộ trình học
     */
    public RoadmapResponse toRoadmapResponse(
            LearningPath learningPath,
            List<RoadmapModuleItemResponse> moduleItems,
            boolean passedAllParts
    ) {
        TargetProfile targetProfile = learningPath.getTargetProfile();
        int totalModules = moduleItems.size();

        // Đếm tổng số chặng đã vượt ải hoàn thành trên lộ trình
        int completedModules = (int) moduleItems.stream()
                .filter(item -> item.getStatus() == ELessonStatus.COMPLETED)
                .count();

        // Nếu học viên PASS 100% 7 Part hoặc đã hoàn thành lộ trình -> tiến độ tổng thể là 100%
        int overallProgressPercent;
        if (passedAllParts || learningPath.getStatus() == ELearningPathStatus.COMPLETED) {
            overallProgressPercent = FULL_PERCENT;
        } else if (totalModules > 0) {
            overallProgressPercent = (int) Math.round((completedModules * 100.0) / totalModules);
        } else {
            overallProgressPercent = ZERO_PERCENT;
        }

        return RoadmapResponse.builder()
                .learningPathId(learningPath.getId())
                .version(learningPath.getVersion() != null ? learningPath.getVersion() : 1)
                .status(learningPath.getStatus())
                .targetProfileId(targetProfile != null ? targetProfile.getId() : null)
                .targetScore(targetProfile != null ? targetProfile.getTargetTotalScore() : 0)
                .targetDescription(targetProfile != null ? targetProfile.getDescription() : null)
                .diagnosticAttemptId(learningPath.getDiagnosticAttempt() != null
                        ? learningPath.getDiagnosticAttempt().getId()
                        : null)
                .progressPercent(overallProgressPercent)
                .totalModules(totalModules)
                .completedModules(completedModules)
                .isPassedAllParts(passedAllParts)
                .modules(moduleItems)
                .build();
    }

    /**
     * Chuyển đổi một bản ghi LearningPathItem sang RoadmapModuleItemResponse.
     *
     * @param item             Chặng học trong lộ trình của học viên
     * @param totalLessons     Tổng số bài học đã xuất bản thuộc Module
     * @param completedLessons Số lượng bài học ước tính/đã hoàn thành trong Module
     * @return RoadmapModuleItemResponse đầy đủ thông tin hiển thị Timeline
     */
    public RoadmapModuleItemResponse toRoadmapModuleItemResponse(
            LearningPathItem item,
            int totalLessons,
            int completedLessons
    ) {
        Module module = item.getModule();
        Part part = module != null ? module.getPart() : null;
        Ability ability = item.getAbility();

        int safeProgress = clampPercent(item.getProgressPercent());

        return RoadmapModuleItemResponse.builder()
                .id(module != null ? module.getId() : null)
                .learningPathItemId(item.getId())
                .orderNo(item.getOrderNo())
                .sequence(module != null && module.getSequence() != null ? module.getSequence() : item.getOrderNo())
                .title(module != null ? module.getTitle() : "")
                .description(module != null && module.getDescriptions() != null ? module.getDescriptions() : "")
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(resolveSection(part))
                .abilityId(ability != null ? ability.getId() : null)
                .abilityName(ability != null ? ability.getName() : null)
                .reason(item.getReason())
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercent(safeProgress)
                .status(item.getStatus())
                .statusLabel(resolveStatusLabel(item.getStatus()))
                .build();
    }

    /**
     * Chuyển đổi một bài học (Lesson) sang UserModuleLessonResponse cho màn hình Chi tiết Module (UC10).
     *
     * @param lesson        Thực thể Lesson đã xuất bản
     * @param questionCount Số lượng câu hỏi gắn trong practice_questions (dùng cho cả VIDEO ôn tập và PRACTICE)
     * @param isCompleted   Cờ đánh dấu bài học đã hoàn thành hay chưa
     * @param isLocked      Cờ đánh dấu bài học đang bị khóa hay không
     * @return UserModuleLessonResponse
     */
    public UserModuleLessonResponse toUserModuleLessonResponse(
            Lesson lesson,
            int questionCount,
            boolean isCompleted,
            boolean isLocked
    ) {
        Ability ability = lesson.getAbility();
        int safeDuration = lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0
                ? lesson.getDurationSeconds()
                : 0;

        return UserModuleLessonResponse.builder()
                .id(lesson.getId())
                .orderNo(lesson.getOrderNo())
                .title(lesson.getTitle())
                .type(lesson.getLessonType())
                .durationSeconds(safeDuration)
                .questionCount(Math.max(0, questionCount))
                .isFree(Boolean.TRUE.equals(lesson.getIsFree()))
                .isCompleted(isCompleted)
                .isLocked(isLocked)
                .abilityName(ability != null ? ability.getName() : null)
                .build();
    }

    /**
     * Tổng hợp thông tin Chi tiết Module kèm danh sách bài học và cấu hình bài thi vượt ải.
     *
     * @param item                   Chặng học của học viên trong lộ trình
     * @param lessons                Danh sách bài học đã map
     * @param totalVideoLessons      Tổng số bài giảng Video lý thuyết
     * @param completedVideoLessons  Số bài giảng Video đã hoàn thành
     * @param completedLessons       Tổng số bài học đã hoàn thành
     * @param requiredCompletionRate Tỷ lệ hoàn thành tối thiểu (%) để mở bài thi vượt ải (BR03)
     * @param moduleTestInfo         Thông tin cấu hình động & lịch sử thi vượt ải của học viên
     * @return UserModuleDetailResponse hoàn chỉnh cho giao diện UC10
     */
    public UserModuleDetailResponse toUserModuleDetailResponse(
            LearningPathItem item,
            List<UserModuleLessonResponse> lessons,
            int totalVideoLessons,
            int completedVideoLessons,
            int completedLessons,
            int requiredCompletionRate,
            UserModuleTestInfoResponse moduleTestInfo
    ) {
        Module module = item.getModule();
        Part part = module != null ? module.getPart() : null;
        Ability ability = item.getAbility();

        return UserModuleDetailResponse.builder()
                .moduleId(module != null ? module.getId() : null)
                .title(module != null ? module.getTitle() : "")
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(resolveSection(part))
                .description(module != null && module.getDescriptions() != null ? module.getDescriptions() : "")
                .abilityName(ability != null ? ability.getName() : null)
                .status(item.getStatus())
                .statusLabel(resolveStatusLabel(item.getStatus()))
                .inRoadmap(true)
                .accessMode(ECatalogAccessMode.ROADMAP_OPEN)
                .totalLessons(lessons.size())
                .completedLessons(completedLessons)
                .totalVideoLessons(totalVideoLessons)
                .completedVideoLessons(completedVideoLessons)
                .progressPercent(clampPercent(item.getProgressPercent()))
                .requiredCompletionRate(requiredCompletionRate)
                .lessons(lessons)
                .moduleTest(moduleTestInfo)
                .build();
    }

    /**
     * Tổng hợp thông tin Chi tiết Module ở chế độ Học tự do (FREE_STUDY) dành cho các Mô-đun
     * nằm ngoài lộ trình cá nhân được mở từ Thư viện khóa học 7 Part (UC17 Catalog).
     *
     * @param module                 Thực thể Module đã xuất bản (kèm Part và ModuleAbilities)
     * @param lessons                Danh sách bài học đã xuất bản kèm tiến độ thật của học viên
     * @param totalVideoLessons      Tổng số bài giảng Video lý thuyết trong Module
     * @param completedVideoLessons  Số bài giảng Video đã hoàn thành thực tế
     * @param completedLessons       Tổng số bài học đã hoàn thành thực tế
     * @param requiredCompletionRate Tỷ lệ cấu hình của Module
     * @return {@link UserModuleDetailResponse} ở chế độ Học tự do (không bắt buộc bài thi vượt ải)
     */
    public UserModuleDetailResponse toFreeStudyModuleDetailResponse(
            Module module,
            List<UserModuleLessonResponse> lessons,
            int totalVideoLessons,
            int completedVideoLessons,
            int completedLessons,
            int requiredCompletionRate
    ) {
        Part part = module.getPart();
        int totalLessons = lessons.size();
        int progressPercent = totalLessons > ZERO_PERCENT
                ? clampPercent((int) Math.round((completedLessons * 100.0) / totalLessons))
                : ZERO_PERCENT;

        // Lấy tên năng lực đại diện đầu tiên gắn trên Module (nếu có) để hiển thị ở phần mô tả
        String primaryAbilityName = module.getModuleAbilities() == null
                ? null
                : module.getModuleAbilities().stream()
                .filter(Objects::nonNull)
                .map(ModuleAbility::getAbility)
                .filter(Objects::nonNull)
                .map(Ability::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);

        return UserModuleDetailResponse.builder()
                .moduleId(module.getId())
                .title(module.getTitle())
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(resolveSection(part))
                .description(module.getDescriptions() != null ? module.getDescriptions() : "")
                .abilityName(primaryAbilityName)
                .status(ELessonStatus.UNLOCKED)
                .statusLabel(STATUS_FREE_STUDY_LABEL)
                .inRoadmap(false)
                .accessMode(ECatalogAccessMode.FREE_STUDY)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .totalVideoLessons(totalVideoLessons)
                .completedVideoLessons(completedVideoLessons)
                .progressPercent(progressPercent)
                .requiredCompletionRate(requiredCompletionRate)
                .lessons(lessons)
                .moduleTest(null)
                .build();
    }

    /**
     * Dịch mã trạng thái ELessonStatus sang nhãn Tiếng Việt tự nhiên cho giao diện người dùng.
     */
    public String resolveStatusLabel(ELessonStatus status) {
        if (status == null) {
            return STATUS_LOCKED_LABEL;
        }
        return switch (status) {
            case COMPLETED -> STATUS_COMPLETED_LABEL;
            case IN_PROGRESS -> STATUS_IN_PROGRESS_LABEL;
            case UNLOCKED -> STATUS_UNLOCKED_LABEL;
            case LOCKED -> STATUS_LOCKED_LABEL;
        };
    }

    /**
     * Lấy Section (LISTENING/READING) an toàn từ Part, fallback về READING nếu Part chưa gắn Section.
     */
    private ESection resolveSection(Part part) {
        return (part != null && part.getSections() != null)
                ? part.getSections()
                : ESection.READING;
    }

    /**
     * Chuẩn hóa phần trăm tiến độ trong đoạn an toàn [0, 100].
     */
    private int clampPercent(Integer progressPercent) {
        if (progressPercent == null || progressPercent < ZERO_PERCENT) {
            return ZERO_PERCENT;
        }
        return Math.min(progressPercent, FULL_PERCENT);
    }
}
