package com.example.be.mapper.content;

import com.example.be.dto.response.content.CatalogLessonPreviewResponse;
import com.example.be.dto.response.content.CatalogModuleItemResponse;
import com.example.be.dto.response.content.CatalogPartGroupResponse;
import com.example.be.dto.response.content.CourseCatalogResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mapper chuyên biệt chuyển đổi các Entity (Part, Module, Lesson, LearningPathItem) sang DTO
 * phục vụ màn hình Thư viện khóa học 7 Part TOEIC (UC17 Catalog).
 * Đảm bảo tuân thủ BR13 (không trả về URL video/audio trong đề cương xem trước) và BR10 (giữ nguyên khóa lộ trình).
 */
@Component
public class UserCatalogMapper {

    private static final int ZERO_VALUE = 0;
    private static final int FULL_PERCENT = 100;

    private static final String DEFAULT_PART_NAME = "Chuyên đề tổng hợp";
    private static final String CATEGORY_ROADMAP = "Trọng tâm cần học";
    private static final String CATEGORY_FREE_STUDY = "Đã nắm vững • Học tự do";
    private static final String CATEGORY_PREVIEW_ONLY = "Xem trước đề cương";

    private static final String STATUS_COMPLETED_LABEL = "Đã hoàn thành";
    private static final String STATUS_IN_PROGRESS_LABEL = "Đang học";
    private static final String STATUS_UNLOCKED_LABEL = "Sẵn sàng học";
    private static final String STATUS_LOCKED_LABEL = "Chưa mở khóa";
    private static final String STATUS_FREE_STUDY_LABEL = "Học tự do";
    private static final String STATUS_PREVIEW_LABEL = "Cần thi đánh giá đầu vào";

    private static final String LOCK_REASON_PREVIEW_ONLY =
            "Hãy chọn mục tiêu và hoàn thành bài đánh giá đầu vào để mở khóa lộ trình học phù hợp với bạn.";
    private static final String LOCK_REASON_ROADMAP_SEQUENTIAL =
            "Hoàn thành bài kiểm tra vượt ải của mô-đun trước trên lộ trình để mở khóa nội dung này.";

    /**
     * Chuyển đổi Entity Lesson sang DTO đề cương xem trước (BR13).
     * Tuyệt đối không đưa videoUrl/audioUrl/imageUrl vào DTO để bảo vệ học liệu khi mô-đun đang khóa.
     *
     * @param lesson Thực thể bài học đã xuất bản
     * @return {@link CatalogLessonPreviewResponse}
     */
    public CatalogLessonPreviewResponse toLessonPreviewResponse(Lesson lesson) {
        int safeDuration = lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > ZERO_VALUE
                ? lesson.getDurationSeconds()
                : ZERO_VALUE;

        return CatalogLessonPreviewResponse.builder()
                .id(lesson.getId())
                .orderNo(lesson.getOrderNo())
                .title(lesson.getTitle())
                .type(lesson.getLessonType())
                .durationSeconds(safeDuration)
                .isFree(Boolean.TRUE.equals(lesson.getIsFree()))
                .build();
    }

    /**
     * Chuyển đổi một Module đã xuất bản kèm đề cương bài học và trạng thái đối chiếu lộ trình
     * sang DTO hiển thị thẻ Mô-đun trên Thư viện khóa học.
     *
     * @param module                 Mô-đun đã xuất bản (đã nạp Part và ModuleAbilities)
     * @param publishedLessons       Danh sách bài học đã xuất bản của Mô-đun
     * @param pathItem               Chặng học tương ứng trên lộ trình cá nhân (null nếu ngoài lộ trình hoặc chưa thi chẩn đoán)
     * @param hasLearningPath        Cờ xác nhận học viên đã hoàn thành chẩn đoán và có lộ trình hiện hành hay chưa
     * @param completedLessonsFromDb Số bài học học viên đã hoàn thành thực tế trong bảng lesson_progress
     * @return {@link CatalogModuleItemResponse}
     */
    public CatalogModuleItemResponse toCatalogModuleItemResponse(
            Module module,
            List<Lesson> publishedLessons,
            LearningPathItem pathItem,
            boolean hasLearningPath,
            int completedLessonsFromDb
    ) {
        List<Lesson> safeLessons = publishedLessons != null ? publishedLessons : Collections.emptyList();

        // Trích xuất danh sách tên năng lực (Ability) mà mô-đun rèn luyện, loại bỏ trùng lặp và null
        List<String> abilities = extractAbilityNames(module);

        // Thống kê số bài giảng lý thuyết, số bài thực hành, tổng thời lượng và danh sách đề cương xem trước
        int videoLessonCount = ZERO_VALUE;
        int practiceLessonCount = ZERO_VALUE;
        int totalDurationSeconds = ZERO_VALUE;
        for (Lesson lesson : safeLessons) {
            if (lesson.getLessonType() == ELessonType.VIDEO) {
                videoLessonCount++;
            } else if (lesson.getLessonType() == ELessonType.PRACTICE) {
                practiceLessonCount++;
            }
            if (lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > ZERO_VALUE) {
                totalDurationSeconds += lesson.getDurationSeconds();
            }
        }
        List<CatalogLessonPreviewResponse> lessonPreviews = safeLessons.stream()
                .map(this::toLessonPreviewResponse)
                .toList();

        int totalLessons = safeLessons.size();
        boolean inRoadmap = hasLearningPath && pathItem != null;

        // Xác định chế độ truy cập, nhãn phân loại, cờ mở khóa và lý do khóa theo đúng quy tắc đã chốt
        ECatalogAccessMode accessMode;
        String categoryLabel;
        ELessonStatus status;
        String statusLabel;
        boolean canAccess;
        String lockReason;
        int progressPercent;
        int completedLessons;

        if (!hasLearningPath) {
            // Trường hợp 1: Học viên chưa thi chẩn đoán -> chỉ xem trước đề cương 7 Part
            accessMode = ECatalogAccessMode.PREVIEW_ONLY;
            categoryLabel = CATEGORY_PREVIEW_ONLY;
            status = null;
            statusLabel = STATUS_PREVIEW_LABEL;
            canAccess = false;
            lockReason = LOCK_REASON_PREVIEW_ONLY;
            progressPercent = ZERO_VALUE;
            completedLessons = ZERO_VALUE;
        } else if (inRoadmap) {
            // Trường hợp 2A: Mô-đun nằm trong lộ trình cá nhân -> giữ nguyên trạng thái mở/khóa theo BR10
            status = pathItem.getStatus();
            statusLabel = resolveRoadmapStatusLabel(status);
            categoryLabel = CATEGORY_ROADMAP;
            if (status == ELessonStatus.LOCKED) {
                accessMode = ECatalogAccessMode.ROADMAP_LOCKED;
                canAccess = false;
                lockReason = LOCK_REASON_ROADMAP_SEQUENTIAL;
            } else {
                accessMode = ECatalogAccessMode.ROADMAP_OPEN;
                canAccess = true;
                lockReason = null;
            }
            progressPercent = clampPercent(pathItem.getProgressPercent());
            completedLessons = resolveRoadmapCompletedLessons(pathItem, totalLessons, completedLessonsFromDb);
        } else {
            // Trường hợp 2B: Mô-đun ngoài lộ trình -> mở tự do cho học viên ôn tập bổ trợ
            accessMode = ECatalogAccessMode.FREE_STUDY;
            categoryLabel = CATEGORY_FREE_STUDY;
            status = ELessonStatus.UNLOCKED;
            statusLabel = STATUS_FREE_STUDY_LABEL;
            canAccess = true;
            lockReason = null;
            completedLessons = Math.min(totalLessons, Math.max(ZERO_VALUE, completedLessonsFromDb));
            progressPercent = totalLessons > ZERO_VALUE
                    ? clampPercent((int) Math.round((completedLessons * 100.0) / totalLessons))
                    : ZERO_VALUE;
        }

        Part part = module.getPart();

        return CatalogModuleItemResponse.builder()
                .id(module.getId())
                .sequence(module.getSequence())
                .title(module.getTitle())
                .description(module.getDescriptions() != null ? module.getDescriptions() : "")
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(resolveSection(part))
                .abilities(abilities)
                .inRoadmap(inRoadmap)
                .roadmapOrderNo(inRoadmap ? pathItem.getOrderNo() : null)
                .roadmapReason(inRoadmap ? pathItem.getReason() : null)
                .accessMode(accessMode)
                .categoryLabel(categoryLabel)
                .status(status)
                .statusLabel(statusLabel)
                .canAccess(canAccess)
                .lockReason(lockReason)
                .progressPercent(progressPercent)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .videoLessonCount(videoLessonCount)
                .practiceLessonCount(practiceLessonCount)
                .totalDurationSeconds(totalDurationSeconds)
                .lessonPreviews(lessonPreviews)
                .build();
    }

    /**
     * Đóng gói danh sách Mô-đun thuộc một Phần thi (Part 1 - Part 7) kèm thống kê số lượng theo phân loại.
     *
     * @param part    Thực thể Phần thi TOEIC
     * @param modules Danh sách Mô-đun đã được map thuộc Phần thi này
     * @return {@link CatalogPartGroupResponse}
     */
    public CatalogPartGroupResponse toCatalogPartGroupResponse(
            Part part,
            List<CatalogModuleItemResponse> modules
    ) {
        List<CatalogModuleItemResponse> safeModules = modules != null ? modules : Collections.emptyList();

        int roadmapCount = ZERO_VALUE;
        int completedCount = ZERO_VALUE;
        int freeStudyCount = ZERO_VALUE;

        for (CatalogModuleItemResponse item : safeModules) {
            if (Boolean.TRUE.equals(item.getInRoadmap())) {
                roadmapCount++;
                if (item.getStatus() == ELessonStatus.COMPLETED) {
                    completedCount++;
                }
            } else if (item.getAccessMode() == ECatalogAccessMode.FREE_STUDY) {
                freeStudyCount++;
            }
        }

        return CatalogPartGroupResponse.builder()
                .partId(part.getId())
                .partName(part.getName())
                .partDescription(part.getDescription() != null ? part.getDescription() : "")
                .section(resolveSection(part))
                .totalModules(safeModules.size())
                .roadmapModulesCount(roadmapCount)
                .completedModulesCount(completedCount)
                .freeStudyModulesCount(freeStudyCount)
                .modules(safeModules)
                .build();
    }

    /**
     * Tổng hợp toàn bộ Thư viện khóa học 7 Part kèm thông tin mục tiêu và thống kê toàn cục.
     *
     * @param learningPath Lộ trình hiện hành của học viên (null nếu chưa hoàn thành chẩn đoán)
     * @param partGroups   Danh sách nhóm 7 Part đã được map đầy đủ
     * @return {@link CourseCatalogResponse}
     */
    public CourseCatalogResponse toCourseCatalogResponse(
            LearningPath learningPath,
            List<CatalogPartGroupResponse> partGroups
    ) {
        List<CatalogPartGroupResponse> safeParts = partGroups != null ? partGroups : Collections.emptyList();
        boolean hasLearningPath = learningPath != null;
        TargetProfile targetProfile = hasLearningPath ? learningPath.getTargetProfile() : null;

        int totalModules = ZERO_VALUE;
        int totalLessons = ZERO_VALUE;
        int roadmapModuleCount = ZERO_VALUE;
        int completedRoadmapModuleCount = ZERO_VALUE;
        int freeStudyModuleCount = ZERO_VALUE;

        for (CatalogPartGroupResponse group : safeParts) {
            totalModules += group.getTotalModules();
            roadmapModuleCount += group.getRoadmapModulesCount();
            completedRoadmapModuleCount += group.getCompletedModulesCount();
            freeStudyModuleCount += group.getFreeStudyModulesCount();
            for (CatalogModuleItemResponse moduleItem : group.getModules()) {
                totalLessons += moduleItem.getTotalLessons();
            }
        }

        return CourseCatalogResponse.builder()
                .hasLearningPath(hasLearningPath)
                .learningPathStatus(hasLearningPath ? learningPath.getStatus() : null)
                .targetScore(targetProfile != null ? targetProfile.getTargetTotalScore() : null)
                .targetDescription(targetProfile != null ? targetProfile.getDescription() : null)
                .totalParts(safeParts.size())
                .totalModules(totalModules)
                .totalLessons(totalLessons)
                .roadmapModuleCount(roadmapModuleCount)
                .completedRoadmapModuleCount(completedRoadmapModuleCount)
                .freeStudyModuleCount(freeStudyModuleCount)
                .parts(safeParts)
                .build();
    }

    /**
     * Trích xuất danh sách tên các Năng lực (Ability) đã gắn cho Module, giữ thứ tự ổn định và loại trùng.
     */
    private List<String> extractAbilityNames(Module module) {
        if (module.getModuleAbilities() == null || module.getModuleAbilities().isEmpty()) {
            return Collections.emptyList();
        }
        return module.getModuleAbilities().stream()
                .filter(Objects::nonNull)
                .map(ModuleAbility::getAbility)
                .filter(Objects::nonNull)
                .map(ability -> ability.getName() != null ? ability.getName().trim() : "")
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Xác định số bài học hoàn thành cho mô-đun trong lộ trình (tôn trọng bảo lưu BR06 khi đạt 100%/COMPLETED).
     */
    private int resolveRoadmapCompletedLessons(
            LearningPathItem item,
            int totalLessons,
            int completedLessonsFromDb
    ) {
        if (totalLessons <= ZERO_VALUE) {
            return ZERO_VALUE;
        }
        int itemProgress = item.getProgressPercent() != null ? item.getProgressPercent() : ZERO_VALUE;
        if (item.getStatus() == ELessonStatus.COMPLETED || itemProgress >= FULL_PERCENT) {
            return totalLessons;
        }
        return Math.min(totalLessons, Math.max(ZERO_VALUE, completedLessonsFromDb));
    }

    private String resolveRoadmapStatusLabel(ELessonStatus status) {
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

    private ESection resolveSection(Part part) {
        return (part != null && part.getSections() != null) ? part.getSections() : ESection.READING;
    }

    private int clampPercent(Integer value) {
        if (value == null || value < ZERO_VALUE) {
            return ZERO_VALUE;
        }
        return Math.min(value, FULL_PERCENT);
    }
}
