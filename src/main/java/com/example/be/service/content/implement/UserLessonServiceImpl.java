package com.example.be.service.content.implement;

import com.example.be.dto.request.content.UpdateLessonProgressRequest;
import com.example.be.dto.response.content.LessonProgressResponse;
import com.example.be.dto.response.content.UserLessonDetailResponse;
import com.example.be.dto.response.content.UserLessonNavItemResponse;
import com.example.be.dto.response.content.UserLessonReviewQuestionResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserLessonMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.content.service.UserLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Phòng học bài giảng Video & Ghi nhận tiến độ học (UC11).
 * Đảm bảo các chốt chặn nghiệp vụ cốt lõi:
 * - Chống truy cập chui vào bài thuộc Module đang khóa (BR10) và chống IDOR.
 * - Giám sát thời lượng xem thực tế chống tua & giới hạn biên độ đồng bộ theo thời gian thực (BR09).
 * - Tự động cập nhật tỷ lệ hoàn thành bài giảng Video trên LearningPathItem và mở khóa Module Test (BR03).
 * - Nạp bài học, tiến độ và media câu hỏi ôn tập theo lô chống hoàn toàn N+1 Query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLessonServiceImpl implements UserLessonService {

    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int FALLBACK_REQUIRED_COMPLETION_RATE = 80;
    private static final int DEFAULT_VIDEO_COMPLETION_THRESHOLD = 60;
    private static final int FIRST_SYNC_MAX_DELTA_SECONDS = 60;
    private static final int SINGLE_REQUEST_MAX_ACCEPTED_DELTA = 180;
    private static final int WALL_CLOCK_NETWORK_TOLERANCE_SECONDS = 5;
    private static final double DEFAULT_PLAYBACK_RATE = 1.0;
    private static final double MAX_ALLOWED_PLAYBACK_RATE = 2.0;
    private static final double RATE_SAFETY_MARGIN = 1.1;
    private static final int MIN_SECONDS_WHEN_UNKNOWN_DURATION = 10;

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final UserRepository userRepository;
    private final UserLessonMapper userLessonMapper;

    /**
     * Ngưỡng % xem tích lũy tối thiểu để hoàn thành bài giảng Video (BR09), đọc động từ cấu hình hệ thống.
     */
    @Value("${leby.lesson.video-completion-threshold-percent:60}")
    private int videoCompletionThresholdPercent = DEFAULT_VIDEO_COMPLETION_THRESHOLD;

    /**
     * {@inheritDoc}
     *
     * @throws BaseException 404 RESOURCE_NOT_FOUND nếu bài học không tồn tại/chưa xuất bản/không thuộc lộ trình;
     *                       422 DIAGNOSTIC_NOT_COMPLETED nếu học viên chưa có lộ trình;
     *                       422 CONTENT_LOCKED nếu bài học thuộc mô-đun đang bị khóa (BR10)
     */
    @Override
    @Transactional(readOnly = true)
    public UserLessonDetailResponse getLessonDetailForUser(Long userId, Long lessonId) {
        // Bước 1: Nạp bài học kèm Module, Part, Ability trong 1 câu SQL và kiểm tra đã xuất bản (PUBLISHED)
        Lesson lesson = findPublishedLessonOrThrow(lessonId);
        Module module = lesson.getModule();

        // Bước 2: Xác thực lộ trình hiện hành của học viên và kiểm tra khóa tuần tự BR10 cho Module chứa bài học
        LearningPathItem pathItem = resolveUnlockedPathItemOrThrow(userId, module.getId());

        // Bước 3: Nạp toàn bộ bài học PUBLISHED của Module và toàn bộ LessonProgress của học viên trong 2 câu SQL (0 N+1)
        List<Lesson> modulePublishedLessons = lessonRepository
                .findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(module.getId(), EModuleStatus.PUBLISHED);

        List<Long> moduleLessonIds = modulePublishedLessons.stream()
                .map(Lesson::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, LessonProgress> progressByLessonId = loadProgressMapByLessonIds(userId, moduleLessonIds);
        LessonProgress currentProgress = progressByLessonId.get(lesson.getId());

        // Bước 4: Xác định cờ bảo lưu BR06, tính danh sách bài điều hướng, bài liền trước/sau và thống kê Video của Module
        boolean inheritedCompleted = isAllModuleLessonsInheritedOrDone(pathItem);
        List<UserLessonNavItemResponse> navItems = new ArrayList<>(modulePublishedLessons.size());
        Long prevLessonId = null;
        Lesson nextLesson = null;
        int totalVideoLessons = 0;
        int completedVideoLessons = 0;

        for (int i = 0; i < modulePublishedLessons.size(); i++) {
            Lesson itemLesson = modulePublishedLessons.get(i);
            LessonProgress itemProgress = progressByLessonId.get(itemLesson.getId());
            boolean itemCompleted = inheritedCompleted
                    || (itemProgress != null && Boolean.TRUE.equals(itemProgress.getIsCompleted()));

            if (itemLesson.getLessonType() == ELessonType.VIDEO) {
                totalVideoLessons++;
                if (itemCompleted) {
                    completedVideoLessons++;
                }
            }

            if (Objects.equals(itemLesson.getId(), lesson.getId())) {
                if (i > 0) {
                    prevLessonId = modulePublishedLessons.get(i - 1).getId();
                }
                if (i + 1 < modulePublishedLessons.size()) {
                    nextLesson = modulePublishedLessons.get(i + 1);
                }
            }

            navItems.add(userLessonMapper.toNavItemResponse(itemLesson, itemProgress, inheritedCompleted));
        }

        // Bước 5: Tính trạng thái mở khóa bài thi vượt ải theo BR03 từ ngưỡng cấu hình động của Module
        int requiredCompletionRate = resolveRequiredCompletionRate(module);
        int moduleProgressPercent = resolveEffectiveModuleProgress(
                pathItem,
                totalVideoLessons,
                completedVideoLessons
        );
        boolean moduleTestUnlocked = pathItem.getStatus() == ELessonStatus.COMPLETED
                || moduleProgressPercent >= requiredCompletionRate;

        // Bước 6: Nạp bộ câu hỏi ôn tập củng cố (nếu bài giảng có gắn trong practice_questions) cùng Media theo lô 1 query
        List<UserLessonReviewQuestionResponse> reviewQuestions = loadReviewQuestionsWithoutNPlus1(lesson.getId());

        // Bước 7: Map trọn vẹn DTO phản hồi cho phòng học bài giảng Video
        return userLessonMapper.toUserLessonDetailResponse(
                lesson,
                currentProgress,
                inheritedCompleted,
                resolveVideoCompletionThreshold(),
                moduleProgressPercent,
                completedVideoLessons,
                totalVideoLessons,
                requiredCompletionRate,
                moduleTestUnlocked,
                prevLessonId,
                nextLesson,
                navItems,
                reviewQuestions
        );
    }

    /**
     * {@inheritDoc}
     *
     * @throws BaseException 400 BAD_REQUEST nếu gọi cập nhật tiến độ xem video cho bài không phải loại VIDEO;
     *                       404 RESOURCE_NOT_FOUND nếu bài học không tồn tại/chưa xuất bản/không thuộc lộ trình;
     *                       422 CONTENT_LOCKED nếu bài học thuộc mô-đun đang bị khóa (BR10)
     */
    @Override
    @Transactional
    public LessonProgressResponse updateLessonProgress(
            Long userId,
            Long lessonId,
            UpdateLessonProgressRequest request
    ) {
        // Bước 1: Kiểm tra bài học tồn tại, đã PUBLISHED và đúng loại bài giảng VIDEO
        Lesson lesson = findPublishedLessonOrThrow(lessonId);
        if (lesson.getLessonType() != ELessonType.VIDEO) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Chỉ hỗ trợ ghi nhận tiến độ xem cho bài giảng Video"
            );
        }

        Module module = lesson.getModule();

        // Bước 2: Xác thực lộ trình hiện hành và kiểm soát khóa tuần tự BR10 cho Module
        LearningPathItem pathItem = resolveUnlockedPathItemOrThrow(userId, module.getId());
        int requiredCompletionRate = resolveRequiredCompletionRate(module);
        int previousModuleProgress = pathItem.getProgressPercent() != null ? pathItem.getProgressPercent() : 0;
        boolean wasTestUnlocked = pathItem.getStatus() == ELessonStatus.COMPLETED
                || previousModuleProgress >= requiredCompletionRate;

        // Bước 3: Tìm hoặc khởi tạo bản ghi LessonProgress của cặp (userId, lessonId)
        LocalDateTime now = LocalDateTime.now();
        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> createNewProgress(userId, lesson));

        boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        int effectiveDuration = resolveEffectiveDuration(lesson, request.getReportedDurationSeconds());

        // Bước 4: SECURITY (BR09) — Kiểm soát chống gian lận thời lượng xem dựa trên thời gian thực kể từ lần đồng bộ trước
        int requestedDelta = request.getWatchedSecondsDelta() != null ? request.getWatchedSecondsDelta() : 0;
        int acceptedDelta = validateAndCapDeltaSeconds(
                requestedDelta,
                progress.getLastSyncedAt(),
                now,
                request.getPlaybackRate()
        );

        int previousWatched = progress.getWatchedSeconds() != null ? progress.getWatchedSeconds() : 0;
        int updatedWatched = effectiveDuration > 0
                ? Math.min(effectiveDuration, previousWatched + acceptedDelta)
                : previousWatched + acceptedDelta;

        // Lưu mốc tạm dừng (clamped trong [0, effectiveDuration] nếu biết thời lượng) phục vụ Resume
        int requestedPosition = request.getLastPositionSeconds() != null ? request.getLastPositionSeconds() : 0;
        int clampedPosition = effectiveDuration > 0
                ? Math.min(effectiveDuration, Math.max(0, requestedPosition))
                : Math.max(0, requestedPosition);

        int watchPercent = calculateWatchPercent(updatedWatched, effectiveDuration);
        int thresholdPercent = resolveVideoCompletionThreshold();

        // Bước 5: Đánh giá điều kiện hoàn thành bài giảng theo BR09; giữ nguyên trạng thái nếu đã hoàn thành trước đó (Luồng 4b)
        boolean justCompleted = false;
        boolean reachedCompletionThreshold = effectiveDuration > 0
                ? watchPercent >= thresholdPercent
                : updatedWatched >= MIN_SECONDS_WHEN_UNKNOWN_DURATION;

        if (!wasCompleted && reachedCompletionThreshold) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(now);
            justCompleted = true;
            log.info("[UC11] Học viên userId={} đã tự động hoàn thành lessonId={} (watchPercent={}%, ngưỡng={}%)",
                    userId, lessonId, watchPercent, thresholdPercent);
        }

        progress.setWatchedSeconds(updatedWatched);
        progress.setLastPositionSeconds(clampedPosition);
        // Nếu bài đã hoàn thành từ trước nhưng watchPercent mới thấp hơn ngưỡng, giữ tối đa giữa giá trị cũ và mới
        int previousPercent = progress.getWatchPercent() != null ? progress.getWatchPercent() : 0;
        progress.setWatchPercent(Math.max(previousPercent, watchPercent));
        progress.setLastSyncedAt(now);

        LessonProgress savedProgress = lessonProgressRepository.save(progress);

        // Bước 6: Tự động tính lại tỷ lệ hoàn thành bài giảng Video của cả Module và cập nhật LearningPathItem (BR03)
        List<Lesson> publishedModuleLessons = lessonRepository
                .findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(module.getId(), EModuleStatus.PUBLISHED);
        int totalVideoLessons = (int) publishedModuleLessons.stream()
                .filter(l -> l.getLessonType() == ELessonType.VIDEO)
                .count();

        int completedVideoLessons = (int) lessonProgressRepository.countCompletedLessonsByUserAndModuleAndType(
                userId,
                module.getId(),
                EModuleStatus.PUBLISHED,
                ELessonType.VIDEO
        );

        int recalculatedPercent = totalVideoLessons > 0
                ? Math.min(FULL_PROGRESS_PERCENT, (int) Math.round((completedVideoLessons * 100.0) / totalVideoLessons))
                : (Boolean.TRUE.equals(savedProgress.getIsCompleted()) ? FULL_PROGRESS_PERCENT : 0);

        // Bảo toàn tiến độ 100% nếu chặng học thuộc diện kế thừa bảo lưu theo BR06
        int finalModuleProgress = isAllModuleLessonsInheritedOrDone(pathItem)
                ? Math.max(previousModuleProgress, recalculatedPercent)
                : recalculatedPercent;

        boolean pathItemDirty = false;
        if (!Objects.equals(pathItem.getProgressPercent(), finalModuleProgress)) {
            pathItem.setProgressPercent(finalModuleProgress);
            pathItemDirty = true;
        }
        // Chuyển trạng thái UNLOCKED -> IN_PROGRESS ngay khi học viên bắt đầu xem bài giảng trong Module
        if (pathItem.getStatus() == ELessonStatus.UNLOCKED && (updatedWatched > 0 || clampedPosition > 0)) {
            pathItem.setStatus(ELessonStatus.IN_PROGRESS);
            pathItemDirty = true;
        }
        if (pathItemDirty) {
            learningPathItemRepository.save(pathItem);
        }

        boolean moduleTestUnlocked = pathItem.getStatus() == ELessonStatus.COMPLETED
                || finalModuleProgress >= requiredCompletionRate;
        boolean moduleTestJustUnlocked = !wasTestUnlocked && moduleTestUnlocked;
        if (moduleTestJustUnlocked) {
            log.info("[UC11] Học viên userId={} vừa mở khóa bài thi vượt ải moduleId={} ({}% >= {}%)",
                    userId, module.getId(), finalModuleProgress, requiredCompletionRate);
        }

        // Bước 7: Map kết quả tiến độ bài học và trạng thái mở khóa bài thi vượt ải trả về cho FE
        return userLessonMapper.toLessonProgressResponse(
                savedProgress,
                effectiveDuration,
                thresholdPercent,
                justCompleted,
                finalModuleProgress,
                completedVideoLessons,
                totalVideoLessons,
                requiredCompletionRate,
                moduleTestUnlocked,
                moduleTestJustUnlocked
        );
    }

    /**
     * Tìm bài học đã xuất bản kèm ngữ cảnh Module/Part/Ability; ném 404 nếu không tồn tại hoặc đang DRAFT.
     */
    private Lesson findPublishedLessonOrThrow(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdWithModulePartAndAbility(lessonId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Bài học không tồn tại hoặc đã được gỡ bỏ"
                ));

        if (lesson.getStatus() != EModuleStatus.PUBLISHED || lesson.getModule() == null) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Bài học hiện chưa được xuất bản"
            );
        }
        return lesson;
    }

    /**
     * Xác thực học viên có lộ trình hiện hành và Module chứa bài học không ở trạng thái LOCKED (BR10).
     */
    private LearningPathItem resolveUnlockedPathItemOrThrow(Long userId, Long moduleId) {
        LearningPath learningPath = resolveCurrentLearningPath(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.DIAGNOSTIC_NOT_COMPLETED,
                        "Bạn cần hoàn thành bài kiểm tra chẩn đoán trước khi truy cập nội dung học tập"
                ));

        LearningPathItem pathItem = learningPathItemRepository
                .findByLearningPathIdAndModuleIdWithDetails(learningPath.getId(), moduleId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Bài học không thuộc lộ trình học hiện tại của bạn"
                ));

        if (pathItem.getStatus() == ELessonStatus.LOCKED) {
            log.warn("[UC11] Học viên userId={} cố truy cập bài giảng trong moduleId={} đang bị khóa (BR10)",
                    userId, moduleId);
            throw new BaseException(
                    ErrorCode.CONTENT_LOCKED,
                    "Mô-đun đang bị khóa. Bạn phải hoàn thành các mô-đun phía trước"
            );
        }

        return pathItem;
    }

    /**
     * Ưu tiên chọn lộ trình ACTIVE, fallback sang COMPLETED mới nhất của học viên.
     */
    private Optional<LearningPath> resolveCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepository
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);

        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(path -> path.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> Optional.of(candidates.get(0)));
    }

    /**
     * Nạp tiến độ hàng loạt cho tập bài học trong Module và đánh chỉ mục theo lessonId.
     */
    private Map<Long, LessonProgress> loadProgressMapByLessonIds(Long userId, List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<LessonProgress> progressList = lessonProgressRepository.findByUserIdAndLessonIdIn(userId, lessonIds);
        if (progressList == null || progressList.isEmpty()) {
            return Collections.emptyMap();
        }
        return progressList.stream()
                .filter(lp -> lp.getLesson() != null && lp.getLesson().getId() != null)
                .collect(Collectors.toMap(
                        lp -> lp.getLesson().getId(),
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    /**
     * Nạp toàn bộ câu hỏi ôn tập củng cố của bài giảng kèm Media (cả câu hỏi con và câu hỏi cha parent)
     * bằng tối đa 2 câu SQL (triệt tiêu hoàn toàn N+1 Query).
     */
    private List<UserLessonReviewQuestionResponse> loadReviewQuestionsWithoutNPlus1(Long lessonId) {
        List<PracticeQuestion> practiceQuestions = practiceQuestionRepository
                .findAllWithQuestionAndPartByLessonId(lessonId);

        if (practiceQuestions == null || practiceQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        // Gom toàn bộ questionId và parentId vào 1 tập Set duy nhất để nạp Media bằng đúng 1 câu IN
        Set<Long> questionIdsForMedia = new HashSet<>();
        for (PracticeQuestion pq : practiceQuestions) {
            Question question = pq != null ? pq.getQuestion() : null;
            if (question != null && question.getId() != null) {
                questionIdsForMedia.add(question.getId());
                if (question.getParent() != null && question.getParent().getId() != null) {
                    questionIdsForMedia.add(question.getParent().getId());
                }
            }
        }

        Map<Long, List<QuestionMedia>> mediaByQuestionId = new HashMap<>();
        if (!questionIdsForMedia.isEmpty()) {
            List<QuestionMedia> allMedias = lessonRepository.findQuestionMediasByQuestionIdIn(questionIdsForMedia);
            if (allMedias != null) {
                for (QuestionMedia qm : allMedias) {
                    if (qm != null && qm.getQuestion() != null && qm.getQuestion().getId() != null) {
                        mediaByQuestionId
                                .computeIfAbsent(qm.getQuestion().getId(), key -> new ArrayList<>())
                                .add(qm);
                    }
                }
            }
        }

        return userLessonMapper.toReviewQuestionResponses(practiceQuestions, mediaByQuestionId);
    }

    /**
     * Khởi tạo bản ghi LessonProgress mới gắn tham chiếu proxy User (không tốn thêm câu SELECT bảng users).
     */
    private LessonProgress createNewProgress(Long userId, Lesson lesson) {
        User userRef = userRepository.getReferenceById(userId);
        LessonProgress progress = new LessonProgress();
        progress.setUser(userRef);
        progress.setLesson(lesson);
        progress.setWatchedSeconds(0);
        progress.setLastPositionSeconds(0);
        progress.setWatchPercent(0);
        progress.setIsCompleted(false);
        return progress;
    }

    /**
     * SECURITY (BR09): Giới hạn số giây xem tăng thêm theo thời gian thực trôi qua giữa 2 lần đồng bộ
     * nhằm ngăn chặn hành vi giả mạo request API hoặc tua nhanh video bất hợp lệ.
     */
    private int validateAndCapDeltaSeconds(
            int requestedDelta,
            LocalDateTime lastSyncedAt,
            LocalDateTime now,
            Double playbackRate
    ) {
        if (requestedDelta <= 0) {
            return 0;
        }

        double effectiveRate = playbackRate != null
                ? Math.max(0.5, Math.min(playbackRate, MAX_ALLOWED_PLAYBACK_RATE))
                : DEFAULT_PLAYBACK_RATE;

        int maxAllowed;
        if (lastSyncedAt == null) {
            // Lần đồng bộ đầu tiên chưa có mốc trước đó -> chặn trần tối đa 60 giây
            maxAllowed = FIRST_SYNC_MAX_DELTA_SECONDS;
        } else {
            long elapsedSeconds = Math.max(0L, Duration.between(lastSyncedAt, now).getSeconds());
            // Biên độ hợp lệ = thời gian thực * tốc độ phát * biên an toàn 10% + 5s dung sai mạng
            long computedCap = (long) Math.ceil(elapsedSeconds * effectiveRate * RATE_SAFETY_MARGIN)
                    + WALL_CLOCK_NETWORK_TOLERANCE_SECONDS;
            maxAllowed = (int) Math.min(SINGLE_REQUEST_MAX_ACCEPTED_DELTA, Math.max(0L, computedCap));
        }

        int accepted = Math.min(requestedDelta, maxAllowed);
        if (accepted < requestedDelta) {
            log.warn("[UC11-BR09] Phát hiện delta xem bất thường: requested={}s, accepted={}s (lastSyncedAt={})",
                    requestedDelta, accepted, lastSyncedAt);
        }
        return accepted;
    }

    /**
     * Xác định thời lượng chuẩn của video: ưu tiên giá trị lưu trong DB, chỉ fallback sang trình phát nếu DB bằng 0.
     */
    private int resolveEffectiveDuration(Lesson lesson, Integer reportedDurationSeconds) {
        if (lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0) {
            return lesson.getDurationSeconds();
        }
        return reportedDurationSeconds != null && reportedDurationSeconds > 0 ? reportedDurationSeconds : 0;
    }

    /**
     * Tính tỷ lệ % thời lượng xem thực tế trong đoạn [0, 100].
     */
    private int calculateWatchPercent(int watchedSeconds, int effectiveDuration) {
        if (effectiveDuration <= 0 || watchedSeconds <= 0) {
            return 0;
        }
        int percent = (int) Math.floor((watchedSeconds * 100.0) / effectiveDuration);
        return Math.min(FULL_PROGRESS_PERCENT, Math.max(0, percent));
    }

    /**
     * Tính tỷ lệ tiến độ hiệu lực của Module (ưu tiên giá trị bảo lưu BR06 nếu đã đạt 100% hoặc COMPLETED).
     */
    private int resolveEffectiveModuleProgress(
            LearningPathItem pathItem,
            int totalVideoLessons,
            int completedVideoLessons
    ) {
        int storedProgress = pathItem.getProgressPercent() != null ? pathItem.getProgressPercent() : 0;
        if (isAllModuleLessonsInheritedOrDone(pathItem)) {
            return Math.max(storedProgress, FULL_PROGRESS_PERCENT);
        }
        if (totalVideoLessons <= 0) {
            return storedProgress;
        }
        int livePercent = (int) Math.round((completedVideoLessons * 100.0) / totalVideoLessons);
        return Math.min(FULL_PROGRESS_PERCENT, Math.max(storedProgress, livePercent));
    }

    private int resolveRequiredCompletionRate(Module module) {
        return module.getRequiredCompletionRate() != null && module.getRequiredCompletionRate() > 0
                ? module.getRequiredCompletionRate()
                : FALLBACK_REQUIRED_COMPLETION_RATE;
    }

    private int resolveVideoCompletionThreshold() {
        return videoCompletionThresholdPercent > 0 && videoCompletionThresholdPercent <= FULL_PROGRESS_PERCENT
                ? videoCompletionThresholdPercent
                : DEFAULT_VIDEO_COMPLETION_THRESHOLD;
    }

    private boolean isAllModuleLessonsInheritedOrDone(LearningPathItem item) {
        int progress = item.getProgressPercent() != null ? item.getProgressPercent() : 0;
        return item.getStatus() == ELessonStatus.COMPLETED || progress >= FULL_PROGRESS_PERCENT;
    }
}
