package com.example.be.service.content.implement;

import com.example.be.dto.response.content.RoadmapModuleItemResponse;
import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;
import com.example.be.dto.response.content.UserModuleLessonResponse;
import com.example.be.dto.response.content.UserModuleTestInfoResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserRoadmapMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
    private final LessonProgressRepository lessonProgressRepository;
    private final ModuleRepository moduleRepository;
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
        // Nạp số bài học đã hoàn thành thực tế từ bảng lesson_progress trong 1 câu GROUP BY (chống N+1)
        Map<Long, Integer> completedLessonsMap = loadCompletedLessonCountByModuleIds(userId, moduleIds);

        // Bước 5: Map từng LearningPathItem sang DTO kèm số lượng bài học và tiến độ hoàn thành thực tế
        List<RoadmapModuleItemResponse> moduleResponses = items.stream()
                .map(item -> {
                    Long moduleId = item.getModule() != null ? item.getModule().getId() : null;
                    int totalLessons = moduleId != null ? totalLessonsMap.getOrDefault(moduleId, 0) : 0;
                    int actualCompleted = moduleId != null ? completedLessonsMap.getOrDefault(moduleId, 0) : 0;
                    int completedLessons = resolveCompletedLessons(item, totalLessons, actualCompleted);
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
        Optional<LearningPathItem> pathItemOpt = learningPathItemRepository
                .findByLearningPathIdAndModuleIdWithDetails(learningPath.getId(), moduleId);

        if (pathItemOpt.isEmpty()) {
            // Nhánh mở rộng UC17 Catalog: Mô-đun nằm ngoài lộ trình cá nhân -> cho phép truy cập chế độ Học tự do nếu đã PUBLISHED
            return buildFreeStudyModuleDetail(userId, moduleId);
        }

        LearningPathItem pathItem = pathItemOpt.get();

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

        // Bước 6: Nạp tiến độ hoàn thành thật từ bảng lesson_progress (1 query IN) và thống kê riêng cho bài giảng Video (BR03)
        boolean allLessonsInheritedOrDone = isAllModuleLessonsCompleted(pathItem);
        Set<Long> completedLessonIds = loadCompletedLessonIds(userId, publishedLessons);
        int totalVideoLessons = 0;
        int completedVideoLessons = 0;
        int completedLessons = 0;

        List<UserModuleLessonResponse> lessonResponses = new ArrayList<>(publishedLessons.size());
        for (Lesson lesson : publishedLessons) {
            boolean isVideo = lesson.getLessonType() == ELessonType.VIDEO;
            // Một bài học được xem là hoàn thành nếu đã đạt chuẩn trong lesson_progress hoặc được bảo lưu 100% theo BR06
            boolean isCompleted = allLessonsInheritedOrDone || completedLessonIds.contains(lesson.getId());

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
     * Xây dựng chi tiết một Mô-đun ở chế độ Học tự do (FREE_STUDY) dành cho các Mô-đun đã xuất bản
     * nằm ngoài lộ trình cá nhân (học viên đã đạt chuẩn ở bài chẩn đoán và mở từ Thư viện 7 Part).
     */
    private UserModuleDetailResponse buildFreeStudyModuleDetail(Long userId, Long moduleId) {
        // Chỉ cho phép truy cập các Mô-đun đã PUBLISHED; chặn bản nháp DRAFT hoặc ID không tồn tại
        Module module = moduleRepository
                .findByIdAndStatusWithPartAndAbilities(moduleId, EModuleStatus.PUBLISHED)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Nội dung mô-đun không tồn tại hoặc đã được gỡ bỏ"
                ));

        List<Lesson> publishedLessons = lessonRepository
                .findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(moduleId, EModuleStatus.PUBLISHED);
        Map<Long, Integer> questionCountMap = loadPracticeQuestionCounts(publishedLessons);
        Set<Long> completedLessonIds = loadCompletedLessonIds(userId, publishedLessons);

        int totalVideoLessons = 0;
        int completedVideoLessons = 0;
        int completedLessons = 0;

        List<UserModuleLessonResponse> lessonResponses = new ArrayList<>(publishedLessons.size());
        for (Lesson lesson : publishedLessons) {
            boolean isVideo = lesson.getLessonType() == ELessonType.VIDEO;
            boolean isCompleted = completedLessonIds.contains(lesson.getId());

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
            lessonResponses.add(userRoadmapMapper.toUserModuleLessonResponse(lesson, questionCount, isCompleted, false));
        }

        int requiredCompletionRate = resolveRequiredCompletionRate(module);
        return userRoadmapMapper.toFreeStudyModuleDetailResponse(
                module,
                lessonResponses,
                totalVideoLessons,
                completedVideoLessons,
                completedLessons,
                requiredCompletionRate
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

        // Nạp lịch sử lượt thi vượt ải đã nộp của học viên trên Module để lấy điểm cao nhất và lượt thi gần nhất
        List<ModuleTestAttempt> submittedAttempts = moduleTestAttemptRepository
                .findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(userId, moduleId);

        // QUY TẮC BR06 (bản cập nhật 25/11): Trạng thái đã vượt ải phải căn cứ vào chặng hiện tại trên lộ trình,
        // tuyệt đối không lấy lượt PASS từ AIM cũ thấp hơn để đánh dấu đã đỗ khi học viên vừa nâng AIM
        boolean hasPassed = pathItem.getStatus() == ELessonStatus.COMPLETED
                || Boolean.TRUE.equals(pathItem.getIsCorrect());
        Integer bestScore = null;
        Long lastAttemptId = null;

        if (submittedAttempts != null && !submittedAttempts.isEmpty()) {
            // Danh sách đã sắp xếp giảm dần theo submittedAt -> phần tử đầu tiên là lượt thi gần nhất
            lastAttemptId = submittedAttempts.get(0).getId();

            for (ModuleTestAttempt attempt : submittedAttempts) {
                if (attempt.getScore() != null && (bestScore == null || attempt.getScore() > bestScore)) {
                    bestScore = attempt.getScore();
                }
            }
        }

        // Xác định trường hợp nâng AIM: đã bảo lưu 100% bài giảng và từng có điểm thi quá khứ nhưng chưa pass ở AIM mới
        boolean needsRetakeForNewAim = !hasPassed
                && currentProgress >= FULL_PROGRESS_PERCENT
                && bestScore != null;

        String testTitle = "Bài kiểm tra vượt ải — " + module.getTitle();
        String testDescription;
        if (hasPassed) {
            testDescription = "Bạn đã vượt qua bài kiểm tra của mô-đun này. Bạn có thể xem lại kết quả hoặc thi lại để nâng cao điểm số.";
        } else if (needsRetakeForNewAim) {
            testDescription = "Bạn đã hoàn thành toàn bộ bài giảng. Hãy làm lại bài kiểm tra vượt ải đạt từ "
                    + passScore + "/100 điểm theo mục tiêu mới để mở khóa mô-đun tiếp theo.";
        } else {
            testDescription = "Hoàn thành bài kiểm tra để xác nhận năng lực và mở khóa mô-đun tiếp theo trên lộ trình của bạn.";
        }

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
                .needsRetakeForNewAim(needsRetakeForNewAim)
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
     * Nạp số lượng bài học đã hoàn thành của học viên theo từng Module từ bảng lesson_progress trong 1 query GROUP BY.
     */
    private Map<Long, Integer> loadCompletedLessonCountByModuleIds(Long userId, List<Long> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = lessonProgressRepository.countCompletedLessonsGroupedByModuleId(
                userId,
                moduleIds,
                EModuleStatus.PUBLISHED
        );
        return toCountMap(rows);
    }

    /**
     * Nạp tập hợp ID các bài học đã hoàn thành của học viên trong Module từ bảng lesson_progress bằng 1 query IN.
     */
    private Set<Long> loadCompletedLessonIds(Long userId, List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<LessonProgress> progressList = lessonProgressRepository.findByUserIdAndLessonIdIn(userId, lessonIds);
        if (progressList == null || progressList.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> completedIds = new HashSet<>();
        for (LessonProgress progress : progressList) {
            if (Boolean.TRUE.equals(progress.getIsCompleted())
                    && progress.getLesson() != null
                    && progress.getLesson().getId() != null) {
                completedIds.add(progress.getLesson().getId());
            }
        }
        return completedIds;
    }

    /**
     * Xác định số bài học đã hoàn thành hiển thị trên thẻ Module (UC09):
     * - Nếu chặng đã bảo lưu BR06 (COMPLETED hoặc 100%) -> trả về toàn bộ totalLessons.
     * - Ngược lại sử dụng số lượng thật đếm từ bảng lesson_progress (fallback an toàn theo progressPercent nếu chưa có bản ghi).
     */
    private int resolveCompletedLessons(LearningPathItem item, int totalLessons, int actualCompletedFromDb) {
        if (totalLessons <= 0) {
            return 0;
        }
        if (isAllModuleLessonsCompleted(item)) {
            return totalLessons;
        }
        if (actualCompletedFromDb > 0) {
            return Math.min(totalLessons, actualCompletedFromDb);
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
