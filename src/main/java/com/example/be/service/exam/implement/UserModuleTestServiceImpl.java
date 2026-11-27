package com.example.be.service.exam.implement;

import com.example.be.dto.request.exam.SaveModuleTestDraftRequest;
import com.example.be.dto.request.exam.SubmitModuleTestRequest;
import com.example.be.dto.request.exam.SyncModuleTestTimerRequest;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestQuestionSnapshotItem;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.ModuleTestRoomResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.ModuleTestAnswer;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.exam.ModuleTestUserMapper;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAnswerRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.exam.service.UserModuleTestService;
import com.example.be.service.exam.util.QuestionBatchLoader;
import com.example.be.service.exam.util.UserExamAccessHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai toàn bộ nghiệp vụ Phòng thi vượt ải mô-đun (UC13) và Báo cáo / Xem lại bài thi (UC14).
 * Tuân thủ chặt chẽ các Business Rules:
 * - BR03: Kiểm tra hoàn thành tỷ lệ bài giảng tối thiểu cấu hình trên Module trước khi thi.
 * - BR04: Đọc động điểm đỗ theo mốc mục tiêu AIM từ {@code module_target_thresholds}.
 * - BR05: Thi lại không giới hạn khi chưa đạt; xáo trộn theo cụm câu hỏi và thứ tự đáp án A-B-C-D.
 * - BR07 & Luồng 7/7a: Lưu nháp đáp án, đồng bộ đồng hồ ngưng đọng và khôi phục phiên đang dở.
 * - BR10: Tự động mở khóa mô-đun tiếp theo trên lộ trình khi đạt điểm đỗ; hoàn thành lộ trình ở chặng cuối.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserModuleTestServiceImpl implements UserModuleTestService {

    private static final int DEFAULT_REQUIRED_COMPLETION_RATE = 80;
    private static final int DEFAULT_PASS_SCORE = 65;
    private static final int DEFAULT_DURATION_MINUTES = 25;
    private static final int DEFAULT_QUESTION_COUNT = 25;
    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final long SECONDS_PER_MINUTE = 60L;

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleTestQuestionRepository moduleTestQuestionRepository;
    private final ModuleTestAttemptRepository moduleTestAttemptRepository;
    private final ModuleTestAnswerRepository moduleTestAnswerRepository;
    private final QuestionRepository questionRepository;
    private final ModuleTargetThresholdRepository moduleTargetThresholdRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final QuestionBatchLoader questionBatchLoader;
    private final UserExamAccessHelper userExamAccessHelper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ModuleTestUserMapper moduleTestUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StartModuleTestResponse startModuleTest(Long userId, Long moduleId) {
        // Bước 1: Kiểm tra tồn tại User, Lộ trình hiện hành và điều kiện mở khóa BR10 + BR03
        User user = findUserWithTargetOrThrow(userId);
        LearningPathItem pathItem = userExamAccessHelper.requireUnlockedPathItem(userId, moduleId);
        Module module = findModuleWithAbilitiesOrThrow(moduleId);

        validatePrerequisiteBR03(module, pathItem);

        // Bước 2: Nếu đã có phiên thi IN_PROGRESS đang còn hiệu lực thì tái sử dụng (Luồng 7a)
        Optional<ModuleTestAttempt> existingOpt = moduleTestAttemptRepository
                .findTopByUserIdAndModuleIdAndStatusOrderByStartedAtDesc(
                        userId,
                        moduleId,
                        EAttemptStatus.IN_PROGRESS
                );
        if (existingOpt.isPresent()) {
            ModuleTestAttempt existing = existingOpt.get();
            long remaining = resolveEffectiveRemainingSeconds(existing, module);
            if (remaining > 0) {
                return moduleTestUserMapper.toStartResponse(existing, module);
            }
            // Phiên cũ đã hết giờ trên thực tế -> thu bài tự động theo BR07 trước khi tạo phiên mới
            finalizeTimedOutAttempt(existing, module, pathItem);
        }

        // Bước 3: Chuẩn bị danh sách câu hỏi (hỗ trợ cả MANUAL lẫn RANDOM_BY_ABILITY) và xáo trộn BR05
        List<ModuleTestQuestionSnapshotItem> snapshot = buildQuestionSnapshot(module);
        String snapshotJson = serializeSnapshot(snapshot);

        // Bước 4: Khởi tạo lượt thi ModuleTestAttempt mới ở trạng thái IN_PROGRESS
        int totalSeconds = resolveTotalDurationSeconds(module);
        ModuleTestAttempt attempt = new ModuleTestAttempt();
        attempt.setUser(user);
        attempt.setModule(module);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setIsSubmitted(false);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setRemainingSeconds(totalSeconds);
        attempt.setQuestionSnapshot(snapshotJson);
        attempt = moduleTestAttemptRepository.save(attempt);

        log.info("[UC13] Khởi tạo lượt thi vượt ải attemptId={} cho userId={}, moduleId={}, totalQuestions={}",
                attempt.getId(), userId, moduleId, snapshot.size());

        return moduleTestUserMapper.toStartResponse(attempt, module);
    }

    @Override
    @Transactional
    public ModuleTestRoomResponse getActiveSession(Long userId, Long moduleId) {
        LearningPathItem pathItem = userExamAccessHelper.requireUnlockedPathItem(userId, moduleId);
        Optional<ModuleTestAttempt> activeOpt = moduleTestAttemptRepository
                .findTopByUserIdAndModuleIdAndStatusOrderByStartedAtDesc(
                        userId,
                        moduleId,
                        EAttemptStatus.IN_PROGRESS
                );
        if (activeOpt.isEmpty()) {
            return null;
        }

        ModuleTestAttempt attempt = activeOpt.get();
        Module module = findModuleWithAbilitiesOrThrow(moduleId);
        long remaining = resolveEffectiveRemainingSeconds(attempt, module);
        if (remaining <= 0) {
            finalizeTimedOutAttempt(attempt, module, pathItem);
            return null;
        }

        return buildRoomResponse(userId, module, attempt, remaining, pathItem.getLearningPath());
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleTestRoomResponse getExamRoom(Long userId, Long moduleId, Long attemptId) {
        ModuleTestAttempt attempt = loadOwnedAttemptOrThrow(userId, moduleId, attemptId);
        validateAttemptInProgress(attempt);

        LearningPath learningPath = userExamAccessHelper.requireCurrentLearningPath(userId);
        Module module = findModuleWithAbilitiesOrThrow(moduleId);
        long remaining = resolveEffectiveRemainingSeconds(attempt, module);

        return buildRoomResponse(userId, module, attempt, remaining, learningPath);
    }

    @Override
    @Transactional
    public long syncRemainingTime(Long userId, Long moduleId, SyncModuleTestTimerRequest request) {
        ModuleTestAttempt attempt = loadOwnedAttemptOrThrow(userId, moduleId, request.getAttemptId());
        validateAttemptInProgress(attempt);

        Module module = attempt.getModule();
        int totalAllowed = resolveTotalDurationSeconds(module);
        int currentStored = attempt.getRemainingSeconds() != null
                ? Math.min(attempt.getRemainingSeconds(), totalAllowed)
                : totalAllowed;
        int requested = Math.max(0, request.getRemainingSeconds());
        // Chỉ cho phép giữ nguyên hoặc giảm dần để ngăn client sửa tăng thời gian thi
        int clamped = Math.min(currentStored, requested);

        attempt.setRemainingSeconds(clamped);
        moduleTestAttemptRepository.save(attempt);
        return clamped;
    }

    @Override
    @Transactional
    public void saveDraftAnswers(Long userId, Long moduleId, SaveModuleTestDraftRequest request) {
        ModuleTestAttempt attempt = loadOwnedAttemptOrThrow(userId, moduleId, request.getAttemptId());
        validateAttemptInProgress(attempt);

        List<ModuleTestQuestionSnapshotItem> snapshot = resolveSnapshotOrFallback(attempt);
        Map<Long, ModuleTestQuestionSnapshotItem> snapByQuestionId = snapshot.stream()
                .collect(Collectors.toMap(
                        ModuleTestQuestionSnapshotItem::getQuestionId,
                        item -> item,
                        (first, second) -> first
                ));

        List<Long> validQuestionIds = request.getAnswers().stream()
                .map(SaveModuleTestDraftRequest.DraftAnswerItem::getQuestionId)
                .filter(snapByQuestionId::containsKey)
                .distinct()
                .toList();
        if (validQuestionIds.isEmpty()) {
            return;
        }

        Map<Long, Question> questionById = questionRepository.findAllById(validQuestionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, ModuleTestAnswer> existingByQuestionId = moduleTestAnswerRepository
                .findByModuleTestAttemptIdWithQuestion(attempt.getId())
                .stream()
                .collect(Collectors.toMap(
                        ans -> ans.getQuestion().getId(),
                        ans -> ans,
                        (first, second) -> first
                ));

        List<ModuleTestAnswer> toSave = new ArrayList<>();
        for (SaveModuleTestDraftRequest.DraftAnswerItem item : request.getAnswers()) {
            ModuleTestQuestionSnapshotItem snap = snapByQuestionId.get(item.getQuestionId());
            Question question = questionById.get(item.getQuestionId());
            if (snap == null || question == null) {
                continue;
            }

            String originalSelected = examQuestionMapper.resolveOriginalKeyFromDisplay(
                    item.getSelectedOption(),
                    snap.getOptionMap()
            );
            String originalCorrect = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
            boolean isCorrect = originalSelected != null && originalSelected.equalsIgnoreCase(originalCorrect);

            ModuleTestAnswer answer = existingByQuestionId.getOrDefault(item.getQuestionId(), new ModuleTestAnswer());
            answer.setModuleTestAttempt(attempt);
            answer.setQuestion(question);
            answer.setOrderNo(snap.getOrderNo());
            if (snap.getModuleTestQuestionId() != null) {
                answer.setModuleTestQuestion(moduleTestQuestionRepository.getReferenceById(snap.getModuleTestQuestionId()));
            }
            answer.setUserAnswer(buildUserAnswerJson(originalSelected));
            answer.setIsCorrect(isCorrect);
            answer.setIsFlagged(Boolean.TRUE.equals(item.getIsFlagged()));
            toSave.add(answer);
        }

        if (!toSave.isEmpty()) {
            moduleTestAnswerRepository.saveAll(toSave);
        }
    }

    @Override
    @Transactional
    public SubmitModuleTestResponse submitModuleTest(
            Long userId,
            Long moduleId,
            SubmitModuleTestRequest request
    ) {
        ModuleTestAttempt attempt = loadOwnedAttemptOrThrow(userId, moduleId, request.getAttemptId());
        validateAttemptInProgress(attempt);

        LearningPathItem pathItem = userExamAccessHelper.requireUnlockedPathItem(userId, moduleId);
        Module module = findModuleWithAbilitiesOrThrow(moduleId);

        return gradeAndFinalizeAttempt(attempt, module, pathItem, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleTestPerformanceReportResponse getPerformanceReport(Long userId, Long moduleId, Long attemptId) {
        ModuleTestAttempt currentAttempt = loadOwnedAttemptOrThrow(userId, moduleId, attemptId);
        if (!Boolean.TRUE.equals(currentAttempt.getIsSubmitted())) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Lượt thi này chưa được nộp nên chưa có báo cáo kết quả"
            );
        }

        Module module = currentAttempt.getModule();
        LearningPath learningPath = userExamAccessHelper.requireCurrentLearningPath(userId);
        int passScore = resolveDynamicPassScore(module, resolveTargetProfileId(currentAttempt.getUser(), learningPath));

        List<ModuleTestAttempt> submittedAttempts = moduleTestAttemptRepository
                .findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(userId, module.getId());
        List<ModuleTestAnswer> attemptAnswers = moduleTestAnswerRepository
                .findByModuleTestAttemptIdWithQuestion(attemptId);

        int correctCount = (int) attemptAnswers.stream()
                .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                .count();
        List<ModuleTestQuestionSnapshotItem> snapshot = resolveSnapshotOrFallback(currentAttempt);
        int totalQuestions = !snapshot.isEmpty() ? snapshot.size() : attemptAnswers.size();

        RoadmapProgressOutcome outcome = inspectRoadmapProgression(learningPath, module.getId());

        return moduleTestUserMapper.toPerformanceReport(
                module,
                currentAttempt,
                submittedAttempts,
                passScore,
                correctCount,
                totalQuestions,
                outcome.nextModuleId(),
                outcome.isRoadmapCompleted()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleTestReviewResponse getTestReview(Long userId, Long moduleId, Long attemptId) {
        ModuleTestAttempt attempt = loadOwnedAttemptOrThrow(userId, moduleId, attemptId);
        if (!Boolean.TRUE.equals(attempt.getIsSubmitted())) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Bạn cần hoàn thành và nộp bài thi vượt ải trước khi xem lại đáp án"
            );
        }

        Module module = attempt.getModule();
        LearningPath learningPath = userExamAccessHelper.requireCurrentLearningPath(userId);
        int passScore = resolveDynamicPassScore(module, resolveTargetProfileId(attempt.getUser(), learningPath));

        List<ModuleTestQuestionSnapshotItem> snapshot = resolveSnapshotOrFallback(attempt);
        List<Long> questionIds = snapshot.stream()
                .map(ModuleTestQuestionSnapshotItem::getQuestionId)
                .toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(questionIds);

        Map<Long, ModuleTestAnswer> answerByQuestionId = moduleTestAnswerRepository
                .findByModuleTestAttemptIdWithQuestion(attemptId)
                .stream()
                .collect(Collectors.toMap(
                        ans -> ans.getQuestion().getId(),
                        ans -> ans,
                        (first, second) -> first
                ));

        return moduleTestUserMapper.toReviewResponse(
                module,
                attempt,
                passScore,
                snapshot,
                detailedQuestionMap,
                answerByQuestionId
        );
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private User findUserWithTargetOrThrow(Long userId) {
        return userRepository.findWithTargetProfileById(userId)
                .or(() -> userRepository.findById(userId))
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private Module findModuleWithAbilitiesOrThrow(Long moduleId) {
        return moduleRepository.findByIdWithPartAndAbilities(moduleId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy mô-đun với ID: " + moduleId
                ));
    }

    private ModuleTestAttempt loadOwnedAttemptOrThrow(Long userId, Long moduleId, Long attemptId) {
        ModuleTestAttempt attempt = moduleTestAttemptRepository.findByIdWithModuleAndUser(attemptId)
                .orElseThrow(() -> new BaseException(ErrorCode.MODULE_TEST_ATTEMPT_NOT_FOUND));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập lượt thi vượt ải này");
        }
        if (moduleId != null && moduleId > 0 && !attempt.getModule().getId().equals(moduleId)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Lượt thi không thuộc mô-đun được yêu cầu");
        }
        return attempt;
    }

    private void validateAttemptInProgress(ModuleTestAttempt attempt) {
        if (Boolean.TRUE.equals(attempt.getIsSubmitted()) || attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.MODULE_TEST_ALREADY_SUBMITTED);
        }
    }

    private void validatePrerequisiteBR03(Module module, LearningPathItem pathItem) {
        int requiredRate = module.getRequiredCompletionRate() != null && module.getRequiredCompletionRate() > 0
                ? module.getRequiredCompletionRate()
                : DEFAULT_REQUIRED_COMPLETION_RATE;
        int currentProgress = pathItem.getProgressPercent() != null ? pathItem.getProgressPercent() : 0;

        boolean isUnlocked = pathItem.getStatus() == ELessonStatus.COMPLETED || currentProgress >= requiredRate;
        if (!isUnlocked) {
            throw new BaseException(ErrorCode.MODULE_TEST_PREREQUISITE_NOT_MET);
        }
    }

    private ModuleTestRoomResponse buildRoomResponse(
            Long userId,
            Module module,
            ModuleTestAttempt attempt,
            long remainingSeconds,
            LearningPath learningPath
    ) {
        User user = attempt.getUser() != null ? attempt.getUser() : findUserWithTargetOrThrow(userId);
        int passScore = resolveDynamicPassScore(module, resolveTargetProfileId(user, learningPath));
        int targetScore = resolveTargetTotalScore(user, learningPath);

        List<ModuleTestQuestionSnapshotItem> snapshot = resolveSnapshotOrFallback(attempt);
        List<Long> questionIds = snapshot.stream()
                .map(ModuleTestQuestionSnapshotItem::getQuestionId)
                .toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(questionIds);

        Map<Long, ModuleTestAnswer> draftByQuestionId = moduleTestAnswerRepository
                .findByModuleTestAttemptIdWithQuestion(attempt.getId())
                .stream()
                .collect(Collectors.toMap(
                        ans -> ans.getQuestion().getId(),
                        ans -> ans,
                        (first, second) -> first
                ));

        return moduleTestUserMapper.toRoomResponse(
                module,
                attempt,
                passScore,
                targetScore,
                remainingSeconds,
                snapshot,
                detailedQuestionMap,
                draftByQuestionId
        );
    }

    /**
     * Xây dựng danh sách câu hỏi cho lượt thi mới và áp dụng xáo trộn theo cụm câu hỏi + đáp án A-B-C-D (BR05).
     */
    private List<ModuleTestQuestionSnapshotItem> buildQuestionSnapshot(Module module) {
        List<RawSelectedQuestion> rawItems = selectRawQuestionsForModule(module);
        if (rawItems.isEmpty()) {
            throw new BaseException(ErrorCode.MODULE_TEST_NOT_CONFIGURED);
        }

        boolean shouldShuffle = Boolean.TRUE.equals(module.getShuffleQuestions());
        List<RawSelectedQuestion> orderedItems;
        if (shouldShuffle) {
            orderedItems = shuffleClusterAware(rawItems);
        } else {
            orderedItems = rawItems;
        }

        List<ModuleTestQuestionSnapshotItem> snapshot = new ArrayList<>(orderedItems.size());
        for (int i = 0; i < orderedItems.size(); i++) {
            RawSelectedQuestion raw = orderedItems.get(i);
            Map<String, String> optionMap = shouldShuffle
                    ? examQuestionMapper.buildShuffleOptionMap(raw.question())
                    : Collections.emptyMap();

            snapshot.add(ModuleTestQuestionSnapshotItem.builder()
                    .orderNo(i + 1)
                    .questionId(raw.question().getId())
                    .moduleTestQuestionId(raw.moduleTestQuestionId())
                    .optionMap(optionMap)
                    .build());
        }
        return snapshot;
    }

    private List<RawSelectedQuestion> selectRawQuestionsForModule(Module module) {
        if (module.getTestSelectionMode() == EModuleTestSelectionMode.RANDOM_BY_ABILITY) {
            int targetCount = module.getTestQuestionCount() != null && module.getTestQuestionCount() > 0
                    ? module.getTestQuestionCount()
                    : DEFAULT_QUESTION_COUNT;

            Set<Long> abilityIds = extractModuleAbilityIds(module);
            List<Question> pool = !abilityIds.isEmpty()
                    ? questionRepository.findPlayableQuestionsByAbilityIds(abilityIds)
                    : (module.getPart() != null && module.getPart().getId() != null
                            ? questionRepository.findPlayableQuestionsByPartId(module.getPart().getId())
                            : Collections.emptyList());

            if (pool.size() < targetCount) {
                throw new BaseException(ErrorCode.MODULE_TEST_NOT_CONFIGURED);
            }

            List<RawSelectedQuestion> poolWrapped = pool.stream()
                    .map(q -> new RawSelectedQuestion(q, null))
                    .toList();
            return pickClusterAwareCount(poolWrapped, targetCount);
        }

        List<ModuleTestQuestion> mtqList = moduleTestQuestionRepository
                .findByModuleIdWithQuestionOrderByOrderNoAsc(module.getId());
        if (mtqList.isEmpty()) {
            throw new BaseException(ErrorCode.MODULE_TEST_NOT_CONFIGURED);
        }

        return mtqList.stream()
                .map(mtq -> new RawSelectedQuestion(mtq.getQuestion(), mtq.getId()))
                .toList();
    }

    /**
     * Xáo trộn thứ tự các cụm câu hỏi (câu cùng {@code parentId} luôn đứng liền kề nhau để hiển thị chuẩn trên 1 trang).
     */
    private List<RawSelectedQuestion> shuffleClusterAware(List<RawSelectedQuestion> items) {
        Map<String, List<RawSelectedQuestion>> clustersMap = groupIntoClusters(items);
        List<List<RawSelectedQuestion>> clusters = new ArrayList<>(clustersMap.values());
        Collections.shuffle(clusters);

        List<RawSelectedQuestion> result = new ArrayList<>(items.size());
        for (List<RawSelectedQuestion> cluster : clusters) {
            result.addAll(cluster);
        }
        return result;
    }

    /**
     * Bốc ngẫu nhiên {@code targetCount} câu hỏi từ kho sao cho ưu tiên giữ trọn vẹn các cụm câu chùm (Part 3, 4, 6, 7).
     */
    private List<RawSelectedQuestion> pickClusterAwareCount(List<RawSelectedQuestion> pool, int targetCount) {
        Map<String, List<RawSelectedQuestion>> clustersMap = groupIntoClusters(pool);
        List<List<RawSelectedQuestion>> clusters = new ArrayList<>(clustersMap.values());
        Collections.shuffle(clusters);

        List<RawSelectedQuestion> picked = new ArrayList<>(targetCount);
        List<RawSelectedQuestion> leftover = new ArrayList<>();

        for (List<RawSelectedQuestion> cluster : clusters) {
            if (picked.size() + cluster.size() <= targetCount) {
                picked.addAll(cluster);
            } else {
                leftover.addAll(cluster);
            }
            if (picked.size() == targetCount) {
                break;
            }
        }

        for (RawSelectedQuestion extra : leftover) {
            if (picked.size() >= targetCount) {
                break;
            }
            picked.add(extra);
        }
        return picked;
    }

    private Map<String, List<RawSelectedQuestion>> groupIntoClusters(List<RawSelectedQuestion> items) {
        Map<String, List<RawSelectedQuestion>> clusters = new LinkedHashMap<>();
        for (RawSelectedQuestion item : items) {
            Question q = item.question();
            String key = q.getParent() != null
                    ? "P_" + q.getParent().getId()
                    : "S_" + q.getId();
            clusters.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return clusters;
    }

    private Set<Long> extractModuleAbilityIds(Module module) {
        if (module.getModuleAbilities() == null || module.getModuleAbilities().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (ModuleAbility ma : module.getModuleAbilities()) {
            if (ma.getAbility() != null && ma.getAbility().getId() != null) {
                ids.add(ma.getAbility().getId());
            }
        }
        return ids;
    }

    private void finalizeTimedOutAttempt(ModuleTestAttempt expiredAttempt, Module module, LearningPathItem pathItem) {
        int totalAllowed = resolveTotalDurationSeconds(module);
        SubmitModuleTestRequest autoRequest = SubmitModuleTestRequest.builder()
                .attemptId(expiredAttempt.getId())
                .durationSeconds(totalAllowed)
                .isTimedOut(true)
                .answers(Collections.emptyList())
                .build();
        gradeAndFinalizeAttempt(expiredAttempt, module, pathItem, autoRequest);
    }

    private SubmitModuleTestResponse gradeAndFinalizeAttempt(
            ModuleTestAttempt attempt,
            Module module,
            LearningPathItem pathItem,
            SubmitModuleTestRequest request
    ) {
        List<ModuleTestQuestionSnapshotItem> snapshot = resolveSnapshotOrFallback(attempt);
        List<Long> questionIds = snapshot.stream()
                .map(ModuleTestQuestionSnapshotItem::getQuestionId)
                .toList();

        Map<Long, Question> questionById = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, ModuleTestAnswer> existingByQuestionId = moduleTestAnswerRepository
                .findByModuleTestAttemptIdWithQuestion(attempt.getId())
                .stream()
                .collect(Collectors.toMap(
                        ans -> ans.getQuestion().getId(),
                        ans -> ans,
                        (first, second) -> first
                ));

        // Khử trùng lặp questionId từ payload client để chống gửi lặp câu đúng nhằm gian lận điểm
        Map<Long, SubmitModuleTestRequest.SubmitAnswerItem> submittedByQuestionId = new HashMap<>();
        if (request.getAnswers() != null) {
            for (SubmitModuleTestRequest.SubmitAnswerItem item : request.getAnswers()) {
                if (item != null && item.getQuestionId() != null) {
                    submittedByQuestionId.putIfAbsent(item.getQuestionId(), item);
                }
            }
        }

        List<ModuleTestAnswer> answersToSave = new ArrayList<>(snapshot.size());
        int correctCount = 0;

        for (ModuleTestQuestionSnapshotItem snap : snapshot) {
            Question question = questionById.get(snap.getQuestionId());
            if (question == null) {
                continue;
            }

            ModuleTestAnswer answer = existingByQuestionId.getOrDefault(snap.getQuestionId(), new ModuleTestAnswer());
            SubmitModuleTestRequest.SubmitAnswerItem submitted = submittedByQuestionId.get(snap.getQuestionId());

            String originalSelectedKey;
            boolean isFlagged;

            if (submitted != null) {
                String displayKey = examQuestionMapper.extractAnswerKey(submitted.getUserAnswer());
                originalSelectedKey = examQuestionMapper.resolveOriginalKeyFromDisplay(displayKey, snap.getOptionMap());
                isFlagged = Boolean.TRUE.equals(submitted.getIsFlagged());
            } else {
                // Fallback lấy đáp án nháp đã lưu trước đó trên DB (hữu ích khi hết giờ auto-submit trên server)
                originalSelectedKey = examQuestionMapper.extractAnswerKey(answer.getUserAnswer());
                isFlagged = Boolean.TRUE.equals(answer.getIsFlagged());
            }

            String originalCorrectKey = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
            boolean isCorrect = originalSelectedKey != null && originalSelectedKey.equalsIgnoreCase(originalCorrectKey);
            if (isCorrect) {
                correctCount++;
            }

            answer.setModuleTestAttempt(attempt);
            answer.setQuestion(question);
            answer.setOrderNo(snap.getOrderNo());
            if (snap.getModuleTestQuestionId() != null) {
                answer.setModuleTestQuestion(moduleTestQuestionRepository.getReferenceById(snap.getModuleTestQuestionId()));
            }
            answer.setUserAnswer(buildUserAnswerJson(originalSelectedKey));
            answer.setIsCorrect(isCorrect);
            answer.setIsFlagged(isFlagged);
            answersToSave.add(answer);
        }

        moduleTestAnswerRepository.saveAll(answersToSave);

        int totalQuestions = snapshot.size();
        int score = totalQuestions > 0 ? (int) Math.round((correctCount * 100.0) / totalQuestions) : 0;

        LearningPath learningPath = pathItem.getLearningPath();
        Long targetProfileId = resolveTargetProfileId(attempt.getUser(), learningPath);
        int passScore = resolveDynamicPassScore(module, targetProfileId);
        boolean isPassed = score >= passScore;

        int maxAllowedSeconds = resolveTotalDurationSeconds(module);
        int safeDuration = request.getDurationSeconds() != null
                ? Math.max(1, Math.min(maxAllowedSeconds, request.getDurationSeconds()))
                : maxAllowedSeconds;

        attempt.setScore(score);
        attempt.setResult(isPassed ? ETestResult.PASS : ETestResult.FAIL);
        attempt.setIsSubmitted(true);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setDurationSeconds(safeDuration);
        attempt.setRemainingSeconds(0);
        attempt.setStatus(Boolean.TRUE.equals(request.getIsTimedOut())
                ? EAttemptStatus.TIMED_OUT
                : EAttemptStatus.SUBMITTED);
        moduleTestAttemptRepository.save(attempt);

        RoadmapProgressOutcome progression = isPassed
                ? unlockNextModuleOnPass(learningPath, module.getId())
                : inspectRoadmapProgression(learningPath, module.getId());

        log.info("[UC13] Chấm điểm bài thi vượt ải attemptId={}, moduleId={}, score={}/100, passScore={}, result={}",
                attempt.getId(), module.getId(), score, passScore, attempt.getResult());

        return moduleTestUserMapper.toSubmitResponse(
                attempt,
                passScore,
                correctCount,
                totalQuestions,
                progression.nextModuleId(),
                progression.isRoadmapCompleted()
        );
    }

    /**
     * Thực thi BR10: Khi thi đỗ Module Test -> đánh dấu chặng hiện tại COMPLETED,
     * mở khóa chặng kế tiếp sang IN_PROGRESS, và hoàn tất lộ trình nếu đã qua chặng cuối (UC14 Bước 8).
     */
    private RoadmapProgressOutcome unlockNextModuleOnPass(LearningPath learningPath, Long currentModuleId) {
        List<LearningPathItem> items = learningPathItemRepository
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(learningPath.getId());
        if (items.isEmpty()) {
            return new RoadmapProgressOutcome(null, learningPath.getStatus() == ELearningPathStatus.COMPLETED);
        }

        int currentIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getModule() != null && items.get(i).getModule().getId().equals(currentModuleId)) {
                currentIdx = i;
                break;
            }
        }

        List<LearningPathItem> modified = new ArrayList<>();
        if (currentIdx >= 0) {
            LearningPathItem currentItem = items.get(currentIdx);
            if (currentItem.getStatus() != ELessonStatus.COMPLETED
                    || !Boolean.TRUE.equals(currentItem.getIsCorrect())
                    || currentItem.getProgressPercent() < FULL_PROGRESS_PERCENT) {
                currentItem.setStatus(ELessonStatus.COMPLETED);
                currentItem.setIsCorrect(true);
                currentItem.setProgressPercent(FULL_PROGRESS_PERCENT);
                modified.add(currentItem);
            }

            if (currentIdx + 1 < items.size()) {
                LearningPathItem nextItem = items.get(currentIdx + 1);
                if (nextItem.getStatus() == ELessonStatus.LOCKED) {
                    nextItem.setStatus(ELessonStatus.IN_PROGRESS);
                    modified.add(nextItem);
                }
            }
        }

        if (!modified.isEmpty()) {
            learningPathItemRepository.saveAll(modified);
        }

        boolean allCompleted = items.stream().allMatch(item -> item.getStatus() == ELessonStatus.COMPLETED);
        if (allCompleted && learningPath.getStatus() != ELearningPathStatus.COMPLETED) {
            learningPath.setStatus(ELearningPathStatus.COMPLETED);
            learningPathRepository.save(learningPath);
        }

        Long nextModuleId = (currentIdx >= 0 && currentIdx + 1 < items.size() && items.get(currentIdx + 1).getModule() != null)
                ? items.get(currentIdx + 1).getModule().getId()
                : null;

        return new RoadmapProgressOutcome(nextModuleId, allCompleted);
    }

    private RoadmapProgressOutcome inspectRoadmapProgression(LearningPath learningPath, Long currentModuleId) {
        List<LearningPathItem> items = learningPathItemRepository
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(learningPath.getId());
        if (items.isEmpty()) {
            return new RoadmapProgressOutcome(null, learningPath.getStatus() == ELearningPathStatus.COMPLETED);
        }

        Long nextModuleId = null;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getModule() != null
                    && items.get(i).getModule().getId().equals(currentModuleId)
                    && i + 1 < items.size()
                    && items.get(i + 1).getModule() != null) {
                nextModuleId = items.get(i + 1).getModule().getId();
                break;
            }
        }

        boolean allCompleted = learningPath.getStatus() == ELearningPathStatus.COMPLETED
                || items.stream().allMatch(item -> item.getStatus() == ELessonStatus.COMPLETED);

        return new RoadmapProgressOutcome(nextModuleId, allCompleted);
    }

    private int resolveDynamicPassScore(Module module, Long targetProfileId) {
        if (targetProfileId != null) {
            Optional<ModuleTargetThreshold> thresholdOpt = moduleTargetThresholdRepository
                    .findByModuleIdAndTargetProfileId(module.getId(), targetProfileId);
            if (thresholdOpt.isPresent() && thresholdOpt.get().getPassScore() != null) {
                return thresholdOpt.get().getPassScore();
            }
        }
        return module.getDefaultPassScore() != null ? module.getDefaultPassScore() : DEFAULT_PASS_SCORE;
    }

    private Long resolveTargetProfileId(User user, LearningPath learningPath) {
        if (learningPath != null && learningPath.getTargetProfile() != null) {
            return learningPath.getTargetProfile().getId();
        }
        if (user != null && user.getTargetProfile() != null) {
            return user.getTargetProfile().getId();
        }
        return null;
    }

    private int resolveTargetTotalScore(User user, LearningPath learningPath) {
        TargetProfile profile = learningPath != null && learningPath.getTargetProfile() != null
                ? learningPath.getTargetProfile()
                : (user != null ? user.getTargetProfile() : null);
        return profile != null && profile.getTargetTotalScore() != null ? profile.getTargetTotalScore() : 0;
    }

    private int resolveTotalDurationSeconds(Module module) {
        int minutes = module.getTestDurationMinutes() != null && module.getTestDurationMinutes() > 0
                ? module.getTestDurationMinutes()
                : DEFAULT_DURATION_MINUTES;
        return minutes * (int) SECONDS_PER_MINUTE;
    }

    private long resolveEffectiveRemainingSeconds(ModuleTestAttempt attempt, Module module) {
        int totalAllowed = resolveTotalDurationSeconds(module);
        if (attempt.getRemainingSeconds() != null) {
            return Math.max(0L, Math.min(totalAllowed, attempt.getRemainingSeconds()));
        }
        long elapsed = attempt.getStartedAt() != null
                ? Math.max(0L, ChronoUnit.SECONDS.between(attempt.getStartedAt(), LocalDateTime.now()))
                : 0L;
        return Math.max(0L, totalAllowed - elapsed);
    }

    private List<ModuleTestQuestionSnapshotItem> resolveSnapshotOrFallback(ModuleTestAttempt attempt) {
        if (attempt.getQuestionSnapshot() != null && !attempt.getQuestionSnapshot().isBlank()) {
            try {
                return objectMapper.readValue(
                        attempt.getQuestionSnapshot(),
                        new TypeReference<List<ModuleTestQuestionSnapshotItem>>() {
                        }
                );
            } catch (Exception e) {
                log.warn("Không thể đọc questionSnapshot cho attemptId={}: {}", attempt.getId(), e.getMessage());
            }
        }

        // Fallback cho lượt thi cũ chưa có snapshot: đọc từ module_test_questions theo orderNo gốc
        List<ModuleTestQuestion> mtqList = moduleTestQuestionRepository
                .findByModuleIdWithQuestionOrderByOrderNoAsc(attempt.getModule().getId());
        return mtqList.stream()
                .map(mtq -> ModuleTestQuestionSnapshotItem.builder()
                        .orderNo(mtq.getOrderNo())
                        .questionId(mtq.getQuestion().getId())
                        .moduleTestQuestionId(mtq.getId())
                        .optionMap(Collections.emptyMap())
                        .build())
                .toList();
    }

    private String serializeSnapshot(List<ModuleTestQuestionSnapshotItem> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể khởi tạo dữ liệu đề thi vượt ải");
        }
    }

    private String buildUserAnswerJson(String originalSelectedKey) {
        if (originalSelectedKey == null) {
            return null;
        }
        return "{\"selected\":\"" + originalSelectedKey + "\"}";
    }

    private record RawSelectedQuestion(Question question, Long moduleTestQuestionId) {
    }

    private record RoadmapProgressOutcome(Long nextModuleId, boolean isRoadmapCompleted) {
    }
}
