package com.example.be.service.content.implement;

import com.example.be.dto.response.content.RoadmapModuleItemResponse;
import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;
import com.example.be.dto.response.content.UserModuleLessonResponse;
import com.example.be.dto.response.content.UserModuleTestInfoResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserRoadmapMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.service.content.service.UserRoadmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Triển khai nghiệp vụ Lộ trình học cá nhân (UC09) và Xem chi tiết Module (UC10) cho học viên.
 * Tuân thủ nghiêm ngặt:
 * - Chống N+1 Query bằng JOIN FETCH, @EntityGraph và truy vấn gom nhóm GROUP BY.
 * - Kiểm soát mở khóa tuần tự BR10 và chống IDOR (chỉ đọc lộ trình thuộc userId hiện tại).
 * - Đọc động toàn bộ tham số nghiệp vụ từ DB (requiredCompletionRate, passScore theo AIM, testDurationMinutes, testQuestionCount).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoadmapServiceImpl implements UserRoadmapService {

    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int FALLBACK_REQUIRED_COMPLETION_RATE = 80;
    private static final int FALLBACK_PASS_SCORE = 65;
    private static final int FALLBACK_TEST_DURATION_MINUTES = 25;
    private static final int FALLBACK_TEST_QUESTION_COUNT = 25;

    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final LessonRepository lessonRepository;
    private final ModuleTargetThresholdRepository moduleTargetThresholdRepository;
    private final ModuleTestQuestionRepository moduleTestQuestionRepository;
    private final ModuleTestAttemptRepository moduleTestAttemptRepository;
    private final UserRoadmapMapper userRoadmapMapper;

    /**
     * Lấy lộ trình học tập hiện hành (ACTIVE hoặc COMPLETED mới nhất) của học viên.
     *
     * @param userId ID định danh học viên lấy từ SecurityContext
     * @return {@link RoadmapResponse} hoặc {@code null} nếu học viên chưa có lộ trình
     */
    @Override
    @Transactional(readOnly = true)
    public RoadmapResponse getCurrentUserRoadmap(Long userId) {
        // Bước 1: Tìm lộ trình hiện hành (ưu tiên ACTIVE, fallback sang COMPLETED mới nhất để hỗ trợ UC09 Luồng 4a)
        Optional<LearningPath> currentPathOpt = resolveCurrentLearningPath(userId);
        if (currentPathOpt.isEmpty()) {
            log.info("[UC09] Học viên userId={} chưa có lộ trình học tập hiện hành", userId);
            return null;
        }

        LearningPath learningPath = currentPathOpt.get();

        // Bước 2: Nạp toàn bộ chặng học kèm Module, Part, Ability trong 1 câu SQL duy nhất (triệt tiêu N+1)
        List<LearningPathItem> items = learningPathItemRepository
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(learningPath.getId());

        // Bước 3: Xử lý trường hợp biên học viên PASS 100% 7 Part ở bài chẩn đoán (items rỗng & status = COMPLETED)
        if (items.isEmpty()) {
            boolean passedAllParts = learningPath.getStatus() == ELearningPathStatus.COMPLETED;
            log.info("[UC09] Học viên userId={} có learningPathId={} không có chặng học con (passedAllParts={})",
                    userId, learningPath.getId(), passedAllParts);
            return userRoadmapMapper.toRoadmapResponse(learningPath, Collections.emptyList(), passedAllParts);
        }

        // Bước 4: Gom toàn bộ moduleId và đếm số bài học đã xuất bản (PUBLISHED) bằng đúng 1 câu GROUP BY
        List<Long> moduleIds = items.stream()
                .map(LearningPathItem::getModule)
                .filter(Objects::nonNull)
                .map(Module::getId)
                .distinct()
                .toList();

        Map<Long, Integer> totalLessonsMap = loadPublishedLessonCountByModuleIds(moduleIds);

        // Bước 5: Map từng LearningPathItem sang DTO kèm số lượng bài học và tiến độ hoàn thành
        List<RoadmapModuleItemResponse> moduleResponses = items.stream()
                .map(item -> {
                    Long moduleId = item.getModule() != null ? item.getModule().getId() : null;
                    int totalLessons = moduleId != null ? totalLessonsMap.getOrDefault(moduleId, 0) : 0;
                    int completedLessons = estimateCompletedLessons(item, totalLessons);
                    return userRoadmapMapper.toRoadmapModuleItemResponse(item, totalLessons, completedLessons);
                })
                .toList();

        // Bước 6: Tổng hợp kết quả lộ trình tổng thể và trả về
        return userRoadmapMapper.toRoadmapResponse(learningPath, moduleResponses, false);
    }

    /**
     * Lấy chi tiết một Module thuộc lộ trình học của học viên kèm kiểm soát mở khóa tuần tự (BR10),
     * danh sách bài học đã xuất bản, số câu hỏi thực hành/ôn tập và thông số động bài thi vượt ải.
     *
     * @param userId   ID học viên đang đăng nhập
     * @param moduleId ID của Module cần xem chi tiết
     * @return {@link UserModuleDetailResponse} hoàn chỉnh cho giao diện UC10
     * @throws BaseException 422 DIAGNOSTIC_NOT_COMPLETED nếu chưa có lộ trình;
     *                       404 RESOURCE_NOT_FOUND nếu Module không nằm trong lộ trình;
     *                       422 CONTENT_LOCKED nếu cố truy cập Module đang bị khóa (BR10)
     */
    @Override
    @Transactional(readOnly = true)
    public UserModuleDetailResponse getModuleDetailForUser(Long userId, Long moduleId) {
        // Bước 1: Kiểm tra học viên đã có lộ trình học hiện hành hay chưa
        LearningPath learningPath = resolveCurrentLearningPath(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.DIAGNOSTIC_NOT_COMPLETED,
                        "Bạn cần hoàn thành bài kiểm tra chẩn đoán trước khi truy cập nội dung học tập"
                ));

        // Bước 2: Tìm chặng học tương ứng với moduleId trong lộ trình của chính userId (chống IDOR)
        LearningPathItem pathItem = learningPathItemRepository
                .findByLearningPathIdAndModuleIdWithDetails(learningPath.getId(), moduleId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Nội dung mô-đun không tồn tại hoặc đã được gỡ bỏ"
                ));

        // Bước 3: SECURITY (BR10) — Chặn tuyệt đối hành vi can thiệp URL truy cập mô-đun đang bị khóa
        if (pathItem.getStatus() == ELessonStatus.LOCKED) {
            log.warn("[UC10] Học viên userId={} cố truy cập moduleId={} đang bị khóa (BR10)", userId, moduleId);
            throw new BaseException(
                    ErrorCode.CONTENT_LOCKED,
                    "Mô-đun đang bị khóa. Bạn phải hoàn thành các mô-đun phía trước"
            );
        }

        Module module = pathItem.getModule();

        // Bước 4: Nạp toàn bộ bài học đã xuất bản (PUBLISHED) thuộc Module kèm Ability trong 1 query
        List<Lesson> publishedLessons = lessonRepository
                .findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(moduleId, EModuleStatus.PUBLISHED);

        // Bước 5: Đếm số câu hỏi gắn trong practice_questions cho cả bài VIDEO (ôn tập) và PRACTICE (luyện tập) bằng 1 câu GROUP BY
        Map<Long, Integer> questionCountMap = loadPracticeQuestionCounts(publishedLessons);

        // Bước 6: Tính trạng thái hoàn thành của từng bài học và thống kê riêng cho bài giảng Video (phục vụ BR03)
        boolean allLessonsInheritedOrDone = isAllModuleLessonsCompleted(pathItem);
        int totalVideoLessons = 0;
        int completedVideoLessons = 0;
        int completedLessons = 0;

        List<UserModuleLessonResponse> lessonResponses = new ArrayList<>(publishedLessons.size());
        for (Lesson lesson : publishedLessons) {
            boolean isVideo = lesson.getLessonType() == ELessonType.VIDEO;
            boolean isCompleted = allLessonsInheritedOrDone;

            if (isVideo) {
                totalVideoLessons++;
                if (isCompleted) {
                    completedVideoLessons++;
                }
            }
            if (isCompleted) {
                completedLessons++;
            }

            int questionCount = questionCountMap.getOrDefault(lesson.getId(), 0);
            // Trong một Module đã mở khóa, học viên được chủ động chọn bài giảng hoặc bài tập thực hành
            lessonResponses.add(userRoadmapMapper.toUserModuleLessonResponse(lesson, questionCount, isCompleted, false));
        }

        // Bước 7: Đọc động cấu hình bài thi vượt ải theo AIM (BR03, BR04, V4) và lịch sử thi vượt ải (UC10 Luồng 6b)
        int requiredCompletionRate = resolveRequiredCompletionRate(module);
        UserModuleTestInfoResponse moduleTestInfo = buildModuleTestInfo(
                userId,
                module,
                learningPath.getTargetProfile(),
                pathItem,
                requiredCompletionRate
        );

        // Bước 8: Map dữ liệu hoàn chỉnh trả về cho giao diện Chi tiết Module
        return userRoadmapMapper.toUserModuleDetailResponse(
                pathItem,
                lessonResponses,
                totalVideoLessons,
                completedVideoLessons,
                completedLessons,
                requiredCompletionRate,
                moduleTestInfo
        );
    }

    /**
     * Tìm lộ trình hiện hành của học viên: ưu tiên lộ trình đang ACTIVE,
     * nếu không có ACTIVE thì lấy lộ trình COMPLETED mới nhất (hỗ trợ UC09 Luồng 4a và lộ trình đã hoàn tất 100%).
     */
    private Optional<LearningPath> resolveCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepository
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);

        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        // Ưu tiên tuyệt đối lộ trình đang học (ACTIVE); nếu không còn lộ trình ACTIVE thì lấy bản COMPLETED mới nhất
        return candidates.stream()
                .filter(path -> path.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> Optional.of(candidates.get(0)));
    }

    /**
     * Chuyển kết quả GROUP BY [moduleId, count] thành Map để tra cứu O(1).
     */
    private Map<Long, Integer> loadPublishedLessonCountByModuleIds(List<Long> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = lessonRepository.countByModuleIdInAndStatus(moduleIds, EModuleStatus.PUBLISHED);
        return toCountMap(rows);
    }

    /**
     * Đếm số câu hỏi luyện tập/ôn tập cho toàn bộ danh sách bài học trong 1 câu truy vấn GROUP BY duy nhất.
     */
    private Map<Long, Integer> loadPracticeQuestionCounts(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = lessonRepository.countPracticeQuestionsByLessonIdIn(lessonIds);
        return toCountMap(rows);
    }

    /**
     * Xây dựng thông tin cấu hình động và trạng thái vượt ải của Module Test (BR03, BR04, V4 & UC10 Luồng 6b).
     */
    private UserModuleTestInfoResponse buildModuleTestInfo(
            Long userId,
            Module module,
            TargetProfile targetProfile,
            LearningPathItem pathItem,
            int requiredCompletionRate
    ) {
        Long moduleId = module.getId();
        Long targetProfileId = targetProfile != null ? targetProfile.getId() : null;
        int targetScore = targetProfile != null && targetProfile.getTargetTotalScore() != null
                ? targetProfile.getTargetTotalScore()
                : 0;

        // Đọc động điểm đỗ theo mốc AIM từ module_target_thresholds, fallback sang defaultPassScore của Module
        int passScore = resolvePassScore(module, targetProfileId);

        // Đọc động thời lượng làm bài và số câu hỏi từ các cột cấu hình V4 của Module
        int durationMinutes = module.getTestDurationMinutes() != null && module.getTestDurationMinutes() > 0
                ? module.getTestDurationMinutes()
                : FALLBACK_TEST_DURATION_MINUTES;
        int totalQuestions = resolveTotalTestQuestions(module);

        // Kiểm tra điều kiện mở khóa bài thi vượt ải theo BR03 (hoặc đã hoàn thành mô-đun)
        int currentProgress = pathItem.getProgressPercent() != null ? pathItem.getProgressPercent() : 0;
        boolean isUnlocked = pathItem.getStatus() == ELessonStatus.COMPLETED
                || currentProgress >= requiredCompletionRate;

        // Nạp lịch sử lượt thi vượt ải đã nộp của học viên trên Module để phục vụ UC10 Luồng 6b
        List<ModuleTestAttempt> submittedAttempts = moduleTestAttemptRepository
                .findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(userId, moduleId);

        boolean hasPassed = false;
        Integer bestScore = null;
        Long lastAttemptId = null;

        if (submittedAttempts != null && !submittedAttempts.isEmpty()) {
            // Danh sách đã sắp xếp giảm dần theo submittedAt -> phần tử đầu tiên là lượt thi gần nhất
            lastAttemptId = submittedAttempts.get(0).getId();

            for (ModuleTestAttempt attempt : submittedAttempts) {
                if (attempt.getResult() == ETestResult.PASS) {
                    hasPassed = true;
                }
                if (attempt.getScore() != null && (bestScore == null || attempt.getScore() > bestScore)) {
                    bestScore = attempt.getScore();
                }
            }
        }

        // Nếu chặng đã được bảo lưu COMPLETED từ lộ trình cũ theo BR06 nhưng chưa có attempt mới, vẫn ghi nhận trạng thái đã đạt
        if (!hasPassed && pathItem.getStatus() == ELessonStatus.COMPLETED) {
            hasPassed = true;
        }

        String testTitle = "Bài kiểm tra vượt ải — " + module.getTitle();
        String testDescription = hasPassed
                ? "Bạn đã vượt qua bài kiểm tra của mô-đun này. Bạn có thể xem lại kết quả hoặc thi lại để nâng cao điểm số."
                : "Hoàn thành bài kiểm tra để xác nhận năng lực và mở khóa mô-đun tiếp theo trên lộ trình của bạn.";

        return UserModuleTestInfoResponse.builder()
                .id(moduleId)
                .title(testTitle)
                .description(testDescription)
                .passScore(passScore)
                .targetScore(targetScore)
                .isUnlocked(isUnlocked)
                .totalQuestions(totalQuestions)
                .durationMinutes(durationMinutes)
                .hasPassed(hasPassed)
                .bestScore(bestScore)
                .lastAttemptId(lastAttemptId)
                .build();
    }

    /**
     * Đọc động điểm đỗ vượt ải theo AIM (BR04), tuyệt đối không hardcode.
     */
    private int resolvePassScore(Module module, Long targetProfileId) {
        if (targetProfileId != null) {
            Optional<ModuleTargetThreshold> customThreshold = moduleTargetThresholdRepository
                    .findByModuleIdAndTargetProfileId(module.getId(), targetProfileId);
            if (customThreshold.isPresent() && customThreshold.get().getPassScore() != null) {
                return customThreshold.get().getPassScore();
            }
        }
        return module.getDefaultPassScore() != null ? module.getDefaultPassScore() : FALLBACK_PASS_SCORE;
    }

    /**
     * Đọc động tổng số câu hỏi của bài thi vượt ải dựa trên chế độ cấu hình (V4 — UC A07).
     * - Chế độ RANDOM_BY_ABILITY: dùng testQuestionCount đã cấu hình trên Module.
     * - Chế độ MANUAL: đếm số câu thực tế trong module_test_questions (fallback sang testQuestionCount nếu chưa gán câu nào).
     */
    private int resolveTotalTestQuestions(Module module) {
        int configuredCount = module.getTestQuestionCount() != null && module.getTestQuestionCount() > 0
                ? module.getTestQuestionCount()
                : FALLBACK_TEST_QUESTION_COUNT;

        if (module.getTestSelectionMode() == EModuleTestSelectionMode.RANDOM_BY_ABILITY) {
            return configuredCount;
        }

        List<Object[]> countRows = moduleTestQuestionRepository
                .countQuestionsGroupedByModuleIds(List.of(module.getId()));
        Map<Long, Integer> countMap = toCountMap(countRows);
        int manualCount = countMap.getOrDefault(module.getId(), 0);
        return manualCount > 0 ? manualCount : configuredCount;
    }

    /**
     * Đọc động ngưỡng phần trăm bài giảng cần hoàn thành để mở khóa bài thi vượt ải (BR03).
     */
    private int resolveRequiredCompletionRate(Module module) {
        return module.getRequiredCompletionRate() != null && module.getRequiredCompletionRate() > 0
                ? module.getRequiredCompletionRate()
                : FALLBACK_REQUIRED_COMPLETION_RATE;
    }

    /**
     * Tính số bài học đã hoàn thành dựa trên tiến độ hiện tại của LearningPathItem.
     * NOTE: Khi triển khai UC11 (bảng lesson_progress), chỉ cần thay nguồn đọc tại hàm này bằng LessonProgressRepository.
     */
    private int estimateCompletedLessons(LearningPathItem item, int totalLessons) {
        if (totalLessons <= 0) {
            return 0;
        }
        if (isAllModuleLessonsCompleted(item)) {
            return totalLessons;
        }
        int progress = item.getProgressPercent() != null ? item.getProgressPercent() : 0;
        if (progress <= 0) {
            return 0;
        }
        return (int) Math.min(totalLessons, Math.round((progress / 100.0) * totalLessons));
    }

    /**
     * Xác định toàn bộ bài học trong mô-đun đã hoàn thành hay chưa (bao gồm cả trường hợp bảo lưu BR06 khi tăng AIM).
     */
    private boolean isAllModuleLessonsCompleted(LearningPathItem item) {
        int progress = item.getProgressPercent() != null ? item.getProgressPercent() : 0;
        return item.getStatus() == ELessonStatus.COMPLETED || progress >= FULL_PROGRESS_PERCENT;
    }

    /**
     * Chuyển đổi danh sách mảng [id, count] từ câu truy vấn JPQL GROUP BY thành Map<Long, Integer>.
     */
    private Map<Long, Integer> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Long key = ((Number) row[0]).longValue();
                int count = row[1] != null ? ((Number) row[1]).intValue() : 0;
                result.put(key, count);
            }
        }
        return result;
    }
}
