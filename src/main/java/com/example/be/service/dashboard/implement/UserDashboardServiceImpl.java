package com.example.be.service.dashboard.implement;

import com.example.be.dto.response.dashboard.UserDashboardActivityDto;
import com.example.be.dto.response.dashboard.UserDashboardCurrentModuleDto;
import com.example.be.dto.response.dashboard.UserDashboardDiagnosticDto;
import com.example.be.dto.response.dashboard.UserDashboardLessonItemDto;
import com.example.be.dto.response.dashboard.UserDashboardModuleItemDto;
import com.example.be.dto.response.dashboard.UserDashboardPartEvalDto;
import com.example.be.dto.response.dashboard.UserDashboardProfileDto;
import com.example.be.dto.response.dashboard.UserDashboardStatsDto;
import com.example.be.dto.response.dashboard.UserDashboardSummaryResponse;
import com.example.be.dto.response.dashboard.UserDashboardWeeklyDayDto;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.EDashboardState;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.dashboard.UserDashboardMapper;
import com.example.be.mapper.diagnostic.DiagnosticResultMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticAnswerRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.PartDiagnosticResultRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.dashboard.service.UserDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Triển khai nghiệp vụ Dashboard học tập cá nhân cho học viên (UC08).
 * Tuân thủ nghiêm ngặt:
 * - Xử lý trọn vẹn 5 trạng thái vòng đời (NEED_TARGET, NEED_DIAGNOSTIC, SYNCING_ROADMAP, IN_PROGRESS, COMPLETED).
 * - Tính toán 100% số liệu thật từ Database (tiến độ, Streak theo múi giờ Việt Nam, biểu đồ 7 ngày, điểm dự đoán TOEIC).
 * - Kiểm tra cờ bảo trì học liệu trên mô-đun đang học (Ngoại lệ 7a) và bảo vệ tuần tự BR10.
 * - Triệt tiêu hoàn toàn N+1 Query bằng JOIN FETCH, @EntityGraph, IN và GROUP BY.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardServiceImpl implements UserDashboardService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private static final int WEEKLY_CHART_DAYS = 7;
    private static final int MAX_UPCOMING_MODULES = 4;
    private static final int MAX_UPCOMING_LESSONS = 4;
    private static final int MAX_RECENT_ACTIVITIES = 6;
    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int FALLBACK_REQUIRED_COMPLETION_RATE = 80;
    private static final int TOEIC_MAX_SCORE = 990;
    private static final int TOEIC_SECTION_MAX_SCORE = 495;
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final String MAINTENANCE_MESSAGE =
            "Mô-đun này đang được cập nhật học liệu. Bạn vui lòng quay lại sau hoặc xem các bài giảng khác.";

    private final UserRepository userRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ModuleTestAttemptRepository moduleTestAttemptRepository;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final PartDiagnosticResultRepository partDiagnosticResultRepository;
    private final DiagnosticAnswerRepository diagnosticAnswerRepository;
    private final DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;
    private final DiagnosticResultMapper diagnosticResultMapper;
    private final UserDashboardMapper userDashboardMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDashboardSummaryResponse getStudentDashboardSummary(Long userId) {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDate today = now.toLocalDate();

        // Bước 1: Nạp thông tin học viên kèm mục tiêu AIM hiện hành (1 query @EntityGraph)
        User user = userRepository.findWithTargetProfileById(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thông tin tài khoản người dùng"
                ));

        // Bước 2: Nạp lộ trình hiện hành (ưu tiên ACTIVE, fallback COMPLETED) và toàn bộ lượt thi chẩn đoán/thi thử
        Optional<LearningPath> currentPathOpt = resolveCurrentLearningPath(userId);
        List<DiagnosticAttempt> allDiagnosticAttempts = diagnosticAttemptRepository
                .findAllByUserIdWithContextOrderByRecent(userId);

        DiagnosticAttempt latestCompletedDiagnostic = findLatestCompletedDiagnostic(allDiagnosticAttempts);
        boolean hasInProgressDiagnostic = hasInProgressDiagnostic(allDiagnosticAttempts);

        LearningPath currentPath = currentPathOpt.orElse(null);
        TargetProfile effectiveTarget = currentPath != null && currentPath.getTargetProfile() != null
                ? currentPath.getTargetProfile()
                : user.getTargetProfile();

        UserDashboardProfileDto profileDto = userDashboardMapper.toProfileDto(user, effectiveTarget);

        // Bước 3: Xử lý sớm các trạng thái chưa có lộ trình học (Luồng 2a, 2b và Ngoại lệ 4b)
        if (currentPath == null) {
            if (effectiveTarget == null && latestCompletedDiagnostic == null) {
                log.info("[UC08] Học viên userId={} chưa chọn mục tiêu AIM -> NEED_TARGET", userId);
                return buildOnboardingResponse(
                        EDashboardState.NEED_TARGET,
                        profileDto,
                        buildEmptyDiagnosticDto(false, hasInProgressDiagnostic),
                        Collections.emptyList(),
                        today
                );
            }

            if (latestCompletedDiagnostic == null) {
                log.info("[UC08] Học viên userId={} đã chọn AIM nhưng chưa thi chẩn đoán -> NEED_DIAGNOSTIC", userId);
                return buildOnboardingResponse(
                        EDashboardState.NEED_DIAGNOSTIC,
                        profileDto,
                        buildEmptyDiagnosticDto(false, hasInProgressDiagnostic),
                        Collections.emptyList(),
                        today
                );
            }

            // Ngoại lệ 4b: Đã có bài thi chẩn đoán hoàn thành nhưng lộ trình chưa kịp đồng bộ
            log.warn("[UC08] Học viên userId={} đã nộp attemptId={} nhưng chưa có LearningPath -> SYNCING_ROADMAP",
                    userId, latestCompletedDiagnostic.getId());
            DiagnosticScoreSnapshot scoreSnapshot = loadDiagnosticSnapshot(latestCompletedDiagnostic);
            return buildOnboardingResponse(
                    EDashboardState.SYNCING_ROADMAP,
                    profileDto,
                    buildDiagnosticDto(latestCompletedDiagnostic, hasInProgressDiagnostic, scoreSnapshot),
                    scoreSnapshot.partEvaluations,
                    today
            );
        }

        // Bước 4: Nạp đánh giá 7 Part và điểm TOEIC dự đoán từ bài chẩn đoán gắn với lộ trình (hoặc mới nhất)
        DiagnosticAttempt referenceDiagnostic = resolveReferenceDiagnostic(
                currentPath,
                allDiagnosticAttempts,
                latestCompletedDiagnostic
        );
        DiagnosticScoreSnapshot scoreSnapshot = referenceDiagnostic != null
                ? loadDiagnosticSnapshot(referenceDiagnostic)
                : DiagnosticScoreSnapshot.empty();
        UserDashboardDiagnosticDto diagnosticDto = referenceDiagnostic != null
                ? buildDiagnosticDto(referenceDiagnostic, hasInProgressDiagnostic, scoreSnapshot)
                : buildEmptyDiagnosticDto(false, hasInProgressDiagnostic);

        // Bước 5: Nạp toàn bộ chặng học của lộ trình kèm Module/Part/Ability (1 query JOIN FETCH)
        List<LearningPathItem> pathItems = learningPathItemRepository
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(currentPath.getId());

        int totalModules = pathItems.size();
        int completedModules = (int) pathItems.stream()
                .filter(item -> item.getStatus() == ELessonStatus.COMPLETED)
                .count();
        boolean isPassedAllParts = pathItems.isEmpty() && currentPath.getStatus() == ELearningPathStatus.COMPLETED;

        // Tính tỷ lệ hoàn thành toàn lộ trình an toàn tuyệt đối (chống chia cho 0 khi PASS 100% 7 Part)
        int roadmapProgressPercent;
        if (isPassedAllParts || currentPath.getStatus() == ELearningPathStatus.COMPLETED) {
            roadmapProgressPercent = FULL_PROGRESS_PERCENT;
        } else if (totalModules > 0) {
            roadmapProgressPercent = Math.round((completedModules * 100.0f) / totalModules);
        } else {
            roadmapProgressPercent = 0;
        }

        // Xác định mô-đun đang mở khóa hiện tại theo quy tắc tuần tự BR10 (ưu tiên IN_PROGRESS, fallback UNLOCKED)
        LearningPathItem currentItem = findCurrentActiveItem(pathItems);
        boolean isCompletedRoadmap = currentPath.getStatus() == ELearningPathStatus.COMPLETED
                || isPassedAllParts
                || (totalModules > 0 && completedModules == totalModules && currentItem == null);
        EDashboardState dashboardState = isCompletedRoadmap
                ? EDashboardState.COMPLETED
                : EDashboardState.IN_PROGRESS;

        // Bước 6: Nạp toàn bộ tiến độ bài học và lịch sử thi vượt ải trong 2 câu SQL cố định (0 N+1)
        List<LessonProgress> allLessonProgress = lessonProgressRepository
                .findAllByUserIdWithLessonAndModuleOrderByRecent(userId);
        List<ModuleTestAttempt> allModuleTestAttempts = moduleTestAttemptRepository
                .findAllByUserIdWithModuleOrderByRecent(userId);

        // Lập chỉ mục tiến độ theo lessonId để tra cứu O(1) và gom số bài hoàn thành theo moduleId
        Map<Long, LessonProgress> progressByLessonId = new HashMap<>();
        Map<Long, Integer> completedPublishedLessonsByModuleId = new HashMap<>();
        for (LessonProgress lp : allLessonProgress) {
            if (lp.getLesson() == null || lp.getLesson().getId() == null) {
                continue;
            }
            progressByLessonId.putIfAbsent(lp.getLesson().getId(), lp);
            if (Boolean.TRUE.equals(lp.getIsCompleted())
                    && lp.getLesson().getStatus() == EModuleStatus.PUBLISHED
                    && lp.getLesson().getModule() != null
                    && lp.getLesson().getModule().getId() != null) {
                Long modId = lp.getLesson().getModule().getId();
                completedPublishedLessonsByModuleId.merge(modId, 1, Integer::sum);
            }
        }

        // Đếm tổng số bài học đã xuất bản (PUBLISHED) cho toàn bộ mô-đun trên lộ trình bằng 1 câu GROUP BY
        Map<Long, Integer> totalLessonsByModuleId = loadPublishedLessonCountByModuleIds(pathItems);
        int totalRoadmapLessons = 0;
        int completedRoadmapLessons = 0;
        for (LearningPathItem item : pathItems) {
            if (item.getModule() == null || item.getModule().getId() == null) {
                continue;
            }
            Long modId = item.getModule().getId();
            int modTotal = totalLessonsByModuleId.getOrDefault(modId, 0);
            int actualDone = completedPublishedLessonsByModuleId.getOrDefault(modId, 0);
            // Tôn trọng bảo lưu BR06: nếu chặng đã hoàn thành hoặc đạt 100% thì tính đủ số bài của chặng
            int modCompleted = (item.getStatus() == ELessonStatus.COMPLETED
                    || (item.getProgressPercent() != null && item.getProgressPercent() >= FULL_PROGRESS_PERCENT))
                    ? modTotal
                    : Math.min(modTotal, actualDone);
            totalRoadmapLessons += modTotal;
            completedRoadmapLessons += modCompleted;
        }

        // Bước 7: Tính chuỗi ngày học liên tục (Streak), biểu đồ tiến độ 7 ngày và tổng thời gian học
        StudyMetricsSnapshot studyMetrics = calculateStudyMetrics(
                allLessonProgress,
                allModuleTestAttempts,
                allDiagnosticAttempts,
                today
        );

        // Bước 8: Dựng khối Mô-đun đang học tiếp theo + bài học gợi ý (có kiểm tra bảo trì Ngoại lệ 7a)
        UserDashboardCurrentModuleDto currentModuleDto = currentItem != null
                ? buildCurrentModuleDto(currentItem, progressByLessonId)
                : null;

        // Bước 9: Dựng danh sách tối đa 4 chặng sắp tới và Top 6 hoạt động gần đây
        List<UserDashboardModuleItemDto> upcomingModules = buildUpcomingModules(pathItems, currentItem);
        List<UserDashboardActivityDto> recentActivities = buildRecentActivities(
                allLessonProgress,
                allModuleTestAttempts,
                allDiagnosticAttempts,
                now
        );

        UserDashboardStatsDto statsDto = UserDashboardStatsDto.builder()
                .roadmapProgressPercent(roadmapProgressPercent)
                .completedModules(completedModules)
                .totalModules(totalModules)
                .completedLessons(completedRoadmapLessons)
                .totalLessons(totalRoadmapLessons)
                .streakDays(studyMetrics.streakDays)
                .isStudiedToday(studyMetrics.isStudiedToday)
                .totalStudyMinutes(studyMetrics.totalStudyMinutes)
                .weeklyStudyMinutes(studyMetrics.weeklyStudyMinutes)
                .weeklyActiveDays(studyMetrics.weeklyActiveDays)
                .predictedScore(scoreSnapshot.predictedScore)
                .targetScore(effectiveTarget != null ? effectiveTarget.getTargetTotalScore() : null)
                .isPassedAllParts(isPassedAllParts)
                .canTakeFullMockTest(isCompletedRoadmap)
                .build();

        return UserDashboardSummaryResponse.builder()
                .dashboardState(dashboardState)
                .user(profileDto)
                .diagnostic(diagnosticDto)
                .stats(statsDto)
                .currentModule(currentModuleDto)
                .upcomingModules(upcomingModules)
                .partEvaluations(scoreSnapshot.partEvaluations)
                .weeklyProgress(studyMetrics.weeklyProgress)
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * Xác định lộ trình hiện hành của học viên: ưu tiên lộ trình ACTIVE, fallback COMPLETED mới nhất.
     */
    private Optional<LearningPath> resolveCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepository
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(lp -> lp.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> Optional.of(candidates.get(0)));
    }

    /**
     * Tìm lượt thi chẩn đoán đầu vào (ETestType.DIAGNOSTIC) đã hoàn thành gần nhất.
     */
    private DiagnosticAttempt findLatestCompletedDiagnostic(List<DiagnosticAttempt> attempts) {
        return attempts.stream()
                .filter(this::isDiagnosticType)
                .filter(this::isCompletedAttempt)
                .findFirst()
                .orElse(null);
    }

    /**
     * Kiểm tra học viên có phiên thi chẩn đoán đang làm dở (IN_PROGRESS) hay không.
     */
    private boolean hasInProgressDiagnostic(List<DiagnosticAttempt> attempts) {
        return attempts.stream()
                .filter(this::isDiagnosticType)
                .anyMatch(attempt -> attempt.getStatus() == EAttemptStatus.IN_PROGRESS);
    }

    /**
     * Chọn lượt thi chẩn đoán làm căn cứ hiển thị 7 Part trên Dashboard (ưu tiên lượt gắn trên lộ trình hiện hành).
     */
    private DiagnosticAttempt resolveReferenceDiagnostic(
            LearningPath currentPath,
            List<DiagnosticAttempt> allAttempts,
            DiagnosticAttempt latestCompletedDiagnostic
    ) {
        if (currentPath.getDiagnosticAttempt() != null && currentPath.getDiagnosticAttempt().getId() != null) {
            Long pathAttemptId = currentPath.getDiagnosticAttempt().getId();
            Optional<DiagnosticAttempt> matched = allAttempts.stream()
                    .filter(a -> Objects.equals(a.getId(), pathAttemptId))
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        }
        return latestCompletedDiagnostic;
    }

    /**
     * Nạp kết quả 7 Part TOEIC và tính điểm TOEIC dự đoán thang 990 (bội số của 5) đồng nhất với UC07.
     */
    private DiagnosticScoreSnapshot loadDiagnosticSnapshot(DiagnosticAttempt attempt) {
        Long attemptId = attempt.getId();
        List<PartDiagnosticResult> partResults = partDiagnosticResultRepository
                .findByDiagnosticAttemptIdWithPart(attemptId);

        List<UserDashboardPartEvalDto> partEvaluations = partResults.stream()
                .map(userDashboardMapper::toPartEvalDto)
                .sorted(Comparator.comparingInt(UserDashboardPartEvalDto::getPartNo))
                .toList();

        // Nạp thống kê đáp án đúng và tổng số câu hỏi của đề theo Listening/Reading trong 2 query gọn nhẹ
        List<Object[]> answerStats = diagnosticAnswerRepository.findAnswerStatsByAttemptIds(List.of(attemptId));
        int listeningCorrect = 0;
        int readingCorrect = 0;
        int answeredListening = 0;
        int answeredReading = 0;

        for (Object[] row : answerStats) {
            boolean isListening = isListeningSection(row[1], row[2]);
            boolean isCorrect = Boolean.TRUE.equals(row[3]);
            if (isListening) {
                answeredListening++;
                if (isCorrect) {
                    listeningCorrect++;
                }
            } else {
                answeredReading++;
                if (isCorrect) {
                    readingCorrect++;
                }
            }
        }

        int listeningTotal = 0;
        int readingTotal = 0;
        if (attempt.getDiagnosticTest() != null && attempt.getDiagnosticTest().getId() != null) {
            List<Object[]> testQuestionStats = diagnosticTestQuestionRepository
                    .countQuestionsByTestIdsAndSection(List.of(attempt.getDiagnosticTest().getId()));
            for (Object[] row : testQuestionStats) {
                int count = row[3] != null ? ((Number) row[3]).intValue() : 0;
                if (isListeningSection(row[1], row[2])) {
                    listeningTotal += count;
                } else {
                    readingTotal += count;
                }
            }
        }

        // Fallback về số câu đã trả lời nếu đề không có thống kê cấu trúc câu hỏi
        if (listeningTotal == 0 && readingTotal == 0) {
            listeningTotal = answeredListening;
            readingTotal = answeredReading;
        }

        int totalQuestions = listeningTotal + readingTotal;
        int totalCorrect = listeningCorrect + readingCorrect;

        Integer predictedScore;
        Integer listeningScore;
        Integer readingScore;
        if (totalQuestions > 0) {
            predictedScore = diagnosticResultMapper.calculateScaledToeicScore(
                    totalCorrect, totalQuestions, TOEIC_MAX_SCORE
            );
            listeningScore = diagnosticResultMapper.calculateScaledToeicScore(
                    listeningCorrect, listeningTotal, TOEIC_SECTION_MAX_SCORE
            );
            readingScore = diagnosticResultMapper.calculateScaledToeicScore(
                    readingCorrect, readingTotal, TOEIC_SECTION_MAX_SCORE
            );
        } else if (!partEvaluations.isEmpty()) {
            // Dự phòng khi không có bảng câu trả lời chi tiết: quy đổi trung bình % 7 Part sang thang 990
            double avgPercent = partEvaluations.stream()
                    .mapToInt(UserDashboardPartEvalDto::getScorePercent)
                    .average()
                    .orElse(0.0);
            predictedScore = diagnosticResultMapper.calculateScaledToeicScore(
                    (int) Math.round(avgPercent), FULL_PROGRESS_PERCENT, TOEIC_MAX_SCORE
            );
            listeningScore = null;
            readingScore = null;
        } else {
            predictedScore = 0;
            listeningScore = 0;
            readingScore = 0;
        }

        return new DiagnosticScoreSnapshot(predictedScore, listeningScore, readingScore, partEvaluations);
    }

    /**
     * Tìm mô-đun đang mở khóa hiện tại trên lộ trình tuần tự (ưu tiên IN_PROGRESS, fallback UNLOCKED).
     */
    private LearningPathItem findCurrentActiveItem(List<LearningPathItem> items) {
        return items.stream()
                .filter(item -> item.getStatus() == ELessonStatus.IN_PROGRESS)
                .findFirst()
                .orElseGet(() -> items.stream()
                        .filter(item -> item.getStatus() == ELessonStatus.UNLOCKED)
                        .findFirst()
                        .orElse(null));
    }

    /**
     * Đếm số bài học đã xuất bản (PUBLISHED) theo từng moduleId trong 1 câu SQL GROUP BY.
     */
    private Map<Long, Integer> loadPublishedLessonCountByModuleIds(List<LearningPathItem> pathItems) {
        List<Long> moduleIds = pathItems.stream()
                .map(LearningPathItem::getModule)
                .filter(Objects::nonNull)
                .map(Module::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> countMap = new HashMap<>();
        List<Object[]> rows = lessonRepository.countByModuleIdInAndStatus(moduleIds, EModuleStatus.PUBLISHED);
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                countMap.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return countMap;
    }

    /**
     * Dựng thông tin khối Mô-đun đang học tiếp theo cùng danh sách tối đa 4 bài học gần nhất (UC08 Bước 4, 5, 7 & 7a).
     */
    private UserDashboardCurrentModuleDto buildCurrentModuleDto(
            LearningPathItem currentItem,
            Map<Long, LessonProgress> progressByLessonId
    ) {
        Module module = currentItem.getModule();
        int itemProgressPercent = currentItem.getProgressPercent() != null ? currentItem.getProgressPercent() : 0;
        int requiredCompletionRate = (module != null && module.getRequiredCompletionRate() != null)
                ? module.getRequiredCompletionRate()
                : FALLBACK_REQUIRED_COMPLETION_RATE;

        // Ngoại lệ 7a: Nếu Quản trị viên tạm chuyển mô-đun sang DRAFT để bảo trì -> khóa vào học và thông báo rõ
        if (module == null || module.getStatus() != EModuleStatus.PUBLISHED) {
            return userDashboardMapper.toCurrentModuleDto(
                    currentItem,
                    itemProgressPercent,
                    requiredCompletionRate,
                    0,
                    0,
                    false,
                    true,
                    MAINTENANCE_MESSAGE,
                    null,
                    Collections.emptyList()
            );
        }

        List<Lesson> publishedLessons = lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(
                module.getId(),
                EModuleStatus.PUBLISHED
        );

        // Nạp số câu hỏi luyện tập/ôn tập cho các bài trong mô-đun hiện tại bằng 1 câu GROUP BY
        Map<Long, Integer> questionCountByLessonId = loadQuestionCountsByLessonIds(publishedLessons);

        boolean isModuleInheritedComplete = currentItem.getStatus() == ELessonStatus.COMPLETED
                || itemProgressPercent >= FULL_PROGRESS_PERCENT;
        int completedCount = 0;
        Lesson inProgressLesson = null;
        Lesson firstIncompleteLesson = null;

        for (Lesson lesson : publishedLessons) {
            LessonProgress progress = progressByLessonId.get(lesson.getId());
            boolean isCompleted = isModuleInheritedComplete
                    || (progress != null && Boolean.TRUE.equals(progress.getIsCompleted()));
            if (isCompleted) {
                completedCount++;
                continue;
            }
            if (firstIncompleteLesson == null) {
                firstIncompleteLesson = lesson;
            }
            // Ưu tiên bài học viên đã bắt đầu xem dở để nút "Tiếp tục bài học" trỏ đúng chỗ đang dừng
            if (inProgressLesson == null
                    && progress != null
                    && progress.getWatchedSeconds() != null
                    && progress.getWatchedSeconds() > 0) {
                inProgressLesson = lesson;
            }
        }

        Lesson continueLesson = inProgressLesson != null
                ? inProgressLesson
                : (firstIncompleteLesson != null
                ? firstIncompleteLesson
                : (!publishedLessons.isEmpty() ? publishedLessons.get(0) : null));

        // Ưu tiên hiển thị các bài chưa hoàn thành trước (tối đa 4 bài), nếu đã xong hết thì lấy 4 bài đầu
        List<Lesson> displayLessons = selectDisplayLessons(publishedLessons, progressByLessonId, isModuleInheritedComplete);
        Long continueLessonId = continueLesson != null ? continueLesson.getId() : null;

        List<UserDashboardLessonItemDto> upcomingLessonDtos = displayLessons.stream()
                .map(lesson -> {
                    LessonProgress progress = progressByLessonId.get(lesson.getId());
                    boolean isCompleted = isModuleInheritedComplete
                            || (progress != null && Boolean.TRUE.equals(progress.getIsCompleted()));
                    int questionCount = questionCountByLessonId.getOrDefault(lesson.getId(), 0);
                    boolean isNextRecommended = Objects.equals(lesson.getId(), continueLessonId);
                    return userDashboardMapper.toLessonItemDto(
                            lesson,
                            progress,
                            questionCount,
                            isCompleted,
                            isNextRecommended
                    );
                })
                .toList();

        boolean isModuleTestUnlocked = currentItem.getStatus() == ELessonStatus.COMPLETED
                || itemProgressPercent >= requiredCompletionRate;

        return userDashboardMapper.toCurrentModuleDto(
                currentItem,
                itemProgressPercent,
                requiredCompletionRate,
                completedCount,
                publishedLessons.size(),
                isModuleTestUnlocked,
                false,
                null,
                continueLesson,
                upcomingLessonDtos
        );
    }

    /**
     * Chọn tối đa 4 bài học hiển thị ngay trên thẻ mô-đun hiện tại (ưu tiên bài đang học & chưa hoàn thành).
     */
    private List<Lesson> selectDisplayLessons(
            List<Lesson> publishedLessons,
            Map<Long, LessonProgress> progressByLessonId,
            boolean isModuleInheritedComplete
    ) {
        if (publishedLessons.size() <= MAX_UPCOMING_LESSONS) {
            return publishedLessons;
        }
        List<Lesson> incomplete = new ArrayList<>();
        List<Lesson> completed = new ArrayList<>();
        for (Lesson lesson : publishedLessons) {
            LessonProgress lp = progressByLessonId.get(lesson.getId());
            boolean done = isModuleInheritedComplete || (lp != null && Boolean.TRUE.equals(lp.getIsCompleted()));
            if (done) {
                completed.add(lesson);
            } else {
                incomplete.add(lesson);
            }
        }
        List<Lesson> ordered = new ArrayList<>(MAX_UPCOMING_LESSONS);
        for (Lesson l : incomplete) {
            if (ordered.size() >= MAX_UPCOMING_LESSONS) {
                break;
            }
            ordered.add(l);
        }
        for (Lesson l : completed) {
            if (ordered.size() >= MAX_UPCOMING_LESSONS) {
                break;
            }
            ordered.add(l);
        }
        return ordered;
    }

    /**
     * Đếm số câu hỏi gắn trong practice_questions cho tập hợp bài học trong 1 câu GROUP BY.
     */
    private Map<Long, Integer> loadQuestionCountsByLessonIds(List<Lesson> lessons) {
        List<Long> lessonIds = lessons.stream()
                .map(Lesson::getId)
                .filter(Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> countMap = new HashMap<>();
        List<Object[]> rows = lessonRepository.countPracticeQuestionsByLessonIdIn(lessonIds);
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                countMap.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return countMap;
    }

    /**
     * Chọn tối đa 4 chặng học tiêu biểu quanh mô-đun hiện tại để hiển thị ở cột phải Dashboard.
     */
    private List<UserDashboardModuleItemDto> buildUpcomingModules(
            List<LearningPathItem> pathItems,
            LearningPathItem currentItem
    ) {
        if (pathItems.isEmpty()) {
            return Collections.emptyList();
        }
        int startIndex = 0;
        if (currentItem != null) {
            for (int i = 0; i < pathItems.size(); i++) {
                if (Objects.equals(pathItems.get(i).getId(), currentItem.getId())) {
                    startIndex = i;
                    break;
                }
            }
        } else {
            // Khi đã hoàn thành hết lộ trình -> hiển thị tối đa 4 chặng cuối gần nhất
            startIndex = Math.max(0, pathItems.size() - MAX_UPCOMING_MODULES);
        }

        int endIndex = Math.min(pathItems.size(), startIndex + MAX_UPCOMING_MODULES);
        return pathItems.subList(startIndex, endIndex).stream()
                .map(userDashboardMapper::toModuleItemDto)
                .toList();
    }

    /**
     * Tổng hợp chuỗi ngày học liên tục (Streak), tổng thời gian học và biểu đồ tiến độ 7 ngày gần nhất.
     */
    private StudyMetricsSnapshot calculateStudyMetrics(
            List<LessonProgress> lessonProgressList,
            List<ModuleTestAttempt> moduleTestAttempts,
            List<DiagnosticAttempt> diagnosticAttempts,
            LocalDate today
    ) {
        // Khởi tạo đủ 7 ngày liên tiếp từ (today - 6) đến today để biểu đồ luôn liền mạch
        LocalDate weekStart = today.minusDays(WEEKLY_CHART_DAYS - 1L);
        Map<LocalDate, DailyBucket> dailyBuckets = new LinkedHashMap<>();
        for (int i = 0; i < WEEKLY_CHART_DAYS; i++) {
            LocalDate date = weekStart.plusDays(i);
            dailyBuckets.put(date, new DailyBucket(date));
        }

        Set<LocalDate> activeDates = new HashSet<>();
        long totalStudySeconds = 0L;

        // 1. Tổng hợp thời lượng xem video & bài học hoàn thành từ lesson_progress
        for (LessonProgress lp : lessonProgressList) {
            int watched = lp.getWatchedSeconds() != null ? Math.max(0, lp.getWatchedSeconds()) : 0;
            totalStudySeconds += watched;

            LocalDateTime activityTime = resolveLessonActivityTime(lp);
            if (activityTime != null && (watched > 0 || Boolean.TRUE.equals(lp.getIsCompleted()))) {
                LocalDate activityDate = activityTime.toLocalDate();
                activeDates.add(activityDate);
                DailyBucket bucket = dailyBuckets.get(activityDate);
                if (bucket != null) {
                    bucket.studySeconds += watched;
                }
            }

            if (Boolean.TRUE.equals(lp.getIsCompleted()) && lp.getCompletedAt() != null) {
                LocalDate completedDate = lp.getCompletedAt().toLocalDate();
                activeDates.add(completedDate);
                DailyBucket bucket = dailyBuckets.get(completedDate);
                if (bucket != null) {
                    bucket.completedLessons++;
                }
            }
        }

        // 2. Tổng hợp các lượt thi vượt ải mô-đun đã nộp
        for (ModuleTestAttempt attempt : moduleTestAttempts) {
            if (!Boolean.TRUE.equals(attempt.getIsSubmitted())) {
                continue;
            }
            LocalDateTime submittedAt = attempt.getSubmittedAt() != null
                    ? attempt.getSubmittedAt()
                    : attempt.getCreatedAt();
            if (submittedAt != null) {
                LocalDate date = submittedAt.toLocalDate();
                activeDates.add(date);
                DailyBucket bucket = dailyBuckets.get(date);
                if (bucket != null) {
                    bucket.testsSubmitted++;
                }
            }
        }

        // 3. Tổng hợp các lượt thi chẩn đoán & thi thử đầu ra đã nộp
        for (DiagnosticAttempt attempt : diagnosticAttempts) {
            if (!isCompletedAttempt(attempt)) {
                continue;
            }
            int durationSec = attempt.getDurationSeconds() != null ? Math.max(0, attempt.getDurationSeconds()) : 0;
            totalStudySeconds += durationSec;

            LocalDateTime completedAt = attempt.getCompletedAt() != null
                    ? attempt.getCompletedAt()
                    : attempt.getStartedAt();
            if (completedAt != null) {
                LocalDate date = completedAt.toLocalDate();
                activeDates.add(date);
                DailyBucket bucket = dailyBuckets.get(date);
                if (bucket != null) {
                    bucket.studySeconds += durationSec;
                    bucket.testsSubmitted++;
                }
            }
        }

        // Tính chuỗi ngày học liên tục: hỗ trợ grace period nếu hôm qua có học nhưng hôm nay chưa học
        boolean isStudiedToday = activeDates.contains(today);
        int streakDays = calculateStreakDays(activeDates, today);

        // Chuyển các bucket 7 ngày sang danh sách DTO biểu đồ và cộng tổng phút tuần này
        List<UserDashboardWeeklyDayDto> weeklyProgress = new ArrayList<>(WEEKLY_CHART_DAYS);
        int weeklyStudyMinutes = 0;
        int weeklyActiveDays = 0;
        for (DailyBucket bucket : dailyBuckets.values()) {
            int minutes = toStudyMinutes(bucket.studySeconds);
            boolean hasAnyActivity = minutes > 0 || bucket.completedLessons > 0 || bucket.testsSubmitted > 0;
            if (hasAnyActivity) {
                weeklyActiveDays++;
            }
            weeklyStudyMinutes += minutes;

            weeklyProgress.add(UserDashboardWeeklyDayDto.builder()
                    .date(bucket.date.toString())
                    .dayLabel(userDashboardMapper.resolveVietnameseDayLabel(bucket.date.getDayOfWeek()))
                    .studyMinutes(minutes)
                    .completedLessons(bucket.completedLessons)
                    .testsSubmitted(bucket.testsSubmitted)
                    .isToday(bucket.date.equals(today))
                    .build());
        }

        return new StudyMetricsSnapshot(
                streakDays,
                isStudiedToday,
                toStudyMinutes(totalStudySeconds),
                weeklyStudyMinutes,
                weeklyActiveDays,
                weeklyProgress
        );
    }

    /**
     * Tính số ngày học liên tục (Streak) kết thúc ở hôm nay hoặc hôm qua.
     */
    private int calculateStreakDays(Set<LocalDate> activeDates, LocalDate today) {
        if (activeDates.isEmpty()) {
            return 0;
        }
        LocalDate cursor;
        if (activeDates.contains(today)) {
            cursor = today;
        } else if (activeDates.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0;
        }

        int streak = 0;
        while (activeDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * Hợp nhất và sắp xếp Top 6 hoạt động học tập / khảo thí mới nhất của học viên.
     */
    private List<UserDashboardActivityDto> buildRecentActivities(
            List<LessonProgress> lessonProgressList,
            List<ModuleTestAttempt> moduleTestAttempts,
            List<DiagnosticAttempt> diagnosticAttempts,
            LocalDateTime now
    ) {
        List<UserDashboardActivityDto> activities = new ArrayList<>();

        lessonProgressList.stream()
                .filter(lp -> Boolean.TRUE.equals(lp.getIsCompleted())
                        || (lp.getWatchedSeconds() != null && lp.getWatchedSeconds() > 0))
                .limit(MAX_RECENT_ACTIVITIES)
                .forEach(lp -> {
                    LocalDateTime ts = resolveLessonActivityTime(lp);
                    if (ts != null) {
                        activities.add(userDashboardMapper.toLessonActivityDto(lp, ts, now));
                    }
                });

        moduleTestAttempts.stream()
                .filter(attempt -> Boolean.TRUE.equals(attempt.getIsSubmitted()))
                .limit(MAX_RECENT_ACTIVITIES)
                .forEach(attempt -> {
                    LocalDateTime ts = attempt.getSubmittedAt() != null
                            ? attempt.getSubmittedAt()
                            : attempt.getCreatedAt();
                    if (ts != null) {
                        activities.add(userDashboardMapper.toModuleTestActivityDto(attempt, ts, now));
                    }
                });

        diagnosticAttempts.stream()
                .filter(this::isCompletedAttempt)
                .limit(MAX_RECENT_ACTIVITIES)
                .forEach(attempt -> {
                    LocalDateTime ts = attempt.getCompletedAt() != null
                            ? attempt.getCompletedAt()
                            : attempt.getStartedAt();
                    if (ts != null) {
                        activities.add(userDashboardMapper.toDiagnosticActivityDto(attempt, ts, now));
                    }
                });

        return activities.stream()
                .sorted(Comparator.comparing(
                        UserDashboardActivityDto::getTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_RECENT_ACTIVITIES)
                .toList();
    }

    /**
     * Dựng response gọn gàng cho 3 trạng thái chưa sẵn sàng lộ trình (NEED_TARGET, NEED_DIAGNOSTIC, SYNCING_ROADMAP).
     */
    private UserDashboardSummaryResponse buildOnboardingResponse(
            EDashboardState state,
            UserDashboardProfileDto profileDto,
            UserDashboardDiagnosticDto diagnosticDto,
            List<UserDashboardPartEvalDto> partEvaluations,
            LocalDate today
    ) {
        List<UserDashboardWeeklyDayDto> emptyWeek = new ArrayList<>(WEEKLY_CHART_DAYS);
        LocalDate weekStart = today.minusDays(WEEKLY_CHART_DAYS - 1L);
        for (int i = 0; i < WEEKLY_CHART_DAYS; i++) {
            LocalDate date = weekStart.plusDays(i);
            emptyWeek.add(UserDashboardWeeklyDayDto.builder()
                    .date(date.toString())
                    .dayLabel(userDashboardMapper.resolveVietnameseDayLabel(date.getDayOfWeek()))
                    .studyMinutes(0)
                    .completedLessons(0)
                    .testsSubmitted(0)
                    .isToday(date.equals(today))
                    .build());
        }

        UserDashboardStatsDto emptyStats = UserDashboardStatsDto.builder()
                .roadmapProgressPercent(0)
                .completedModules(0)
                .totalModules(0)
                .completedLessons(0)
                .totalLessons(0)
                .streakDays(0)
                .isStudiedToday(false)
                .totalStudyMinutes(0)
                .weeklyStudyMinutes(0)
                .weeklyActiveDays(0)
                .predictedScore(diagnosticDto.getPredictedScore())
                .targetScore(profileDto.getTargetScore())
                .isPassedAllParts(false)
                .canTakeFullMockTest(false)
                .build();

        return UserDashboardSummaryResponse.builder()
                .dashboardState(state)
                .user(profileDto)
                .diagnostic(diagnosticDto)
                .stats(emptyStats)
                .currentModule(null)
                .upcomingModules(Collections.emptyList())
                .partEvaluations(partEvaluations)
                .weeklyProgress(emptyWeek)
                .recentActivities(Collections.emptyList())
                .build();
    }

    private UserDashboardDiagnosticDto buildEmptyDiagnosticDto(boolean hasCompleted, boolean hasInProgress) {
        return UserDashboardDiagnosticDto.builder()
                .hasCompletedDiagnostic(hasCompleted)
                .hasInProgressDiagnostic(hasInProgress)
                .latestAttemptId(null)
                .predictedScore(null)
                .listeningScore(null)
                .readingScore(null)
                .completedAt(null)
                .build();
    }

    private UserDashboardDiagnosticDto buildDiagnosticDto(
            DiagnosticAttempt attempt,
            boolean hasInProgress,
            DiagnosticScoreSnapshot scoreSnapshot
    ) {
        return UserDashboardDiagnosticDto.builder()
                .hasCompletedDiagnostic(true)
                .hasInProgressDiagnostic(hasInProgress)
                .latestAttemptId(attempt.getId())
                .predictedScore(scoreSnapshot.predictedScore)
                .listeningScore(scoreSnapshot.listeningScore)
                .readingScore(scoreSnapshot.readingScore)
                .completedAt(attempt.getCompletedAt())
                .build();
    }

    private boolean isDiagnosticType(DiagnosticAttempt attempt) {
        return attempt.getDiagnosticTest() == null
                || attempt.getDiagnosticTest().getTestType() == null
                || attempt.getDiagnosticTest().getTestType() == ETestType.DIAGNOSTIC;
    }

    private boolean isCompletedAttempt(DiagnosticAttempt attempt) {
        return attempt.getStatus() == EAttemptStatus.SUBMITTED
                || attempt.getStatus() == EAttemptStatus.TIMED_OUT;
    }

    private boolean isListeningSection(Object sectionObj, Object partNameObj) {
        if (sectionObj instanceof ESection section) {
            return section == ESection.LISTENING;
        }
        if (sectionObj != null && "LISTENING".equalsIgnoreCase(sectionObj.toString())) {
            return true;
        }
        if (partNameObj != null) {
            String name = partNameObj.toString();
            return name.contains("1") || name.contains("2") || name.contains("3") || name.contains("4");
        }
        return false;
    }

    private LocalDateTime resolveLessonActivityTime(LessonProgress lp) {
        if (lp.getLastSyncedAt() != null) {
            return lp.getLastSyncedAt();
        }
        if (lp.getCompletedAt() != null) {
            return lp.getCompletedAt();
        }
        if (lp.getUpdatedAt() != null) {
            return lp.getUpdatedAt();
        }
        return lp.getCreatedAt();
    }

    private int toStudyMinutes(long totalSeconds) {
        if (totalSeconds <= 0L) {
            return 0;
        }
        return Math.max(1, (int) Math.round(totalSeconds / SECONDS_PER_MINUTE));
    }

    private static final class DailyBucket {
        final LocalDate date;
        long studySeconds = 0L;
        int completedLessons = 0;
        int testsSubmitted = 0;

        DailyBucket(LocalDate date) {
            this.date = date;
        }
    }

    private static final class DiagnosticScoreSnapshot {
        final Integer predictedScore;
        final Integer listeningScore;
        final Integer readingScore;
        final List<UserDashboardPartEvalDto> partEvaluations;

        DiagnosticScoreSnapshot(
                Integer predictedScore,
                Integer listeningScore,
                Integer readingScore,
                List<UserDashboardPartEvalDto> partEvaluations
        ) {
            this.predictedScore = predictedScore;
            this.listeningScore = listeningScore;
            this.readingScore = readingScore;
            this.partEvaluations = partEvaluations;
        }

        static DiagnosticScoreSnapshot empty() {
            return new DiagnosticScoreSnapshot(null, null, null, Collections.emptyList());
        }
    }

    private static final class StudyMetricsSnapshot {
        final int streakDays;
        final boolean isStudiedToday;
        final int totalStudyMinutes;
        final int weeklyStudyMinutes;
        final int weeklyActiveDays;
        final List<UserDashboardWeeklyDayDto> weeklyProgress;

        StudyMetricsSnapshot(
                int streakDays,
                boolean isStudiedToday,
                int totalStudyMinutes,
                int weeklyStudyMinutes,
                int weeklyActiveDays,
                List<UserDashboardWeeklyDayDto> weeklyProgress
        ) {
            this.streakDays = streakDays;
            this.isStudiedToday = isStudiedToday;
            this.totalStudyMinutes = totalStudyMinutes;
            this.weeklyStudyMinutes = weeklyStudyMinutes;
            this.weeklyActiveDays = weeklyActiveDays;
            this.weeklyProgress = weeklyProgress;
        }
    }
}
