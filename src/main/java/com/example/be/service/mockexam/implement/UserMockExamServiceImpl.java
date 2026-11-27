package com.example.be.service.mockexam.implement;

import com.example.be.dto.request.mockexam.SaveMockExamDraftRequest;
import com.example.be.dto.request.mockexam.StartMockExamRequest;
import com.example.be.dto.request.mockexam.SubmitMockExamRequest;
import com.example.be.dto.request.mockexam.SyncMockExamTimerRequest;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.dto.response.mockexam.MockExamHistoryItemResponse;
import com.example.be.dto.response.mockexam.MockExamListItemResponse;
import com.example.be.dto.response.mockexam.MockExamListResponse;
import com.example.be.dto.response.mockexam.MockExamReportResponse;
import com.example.be.dto.response.mockexam.MockExamReviewResponse;
import com.example.be.dto.response.mockexam.MockExamRoomResponse;
import com.example.be.dto.response.mockexam.MockExamSubmitResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.mockexam.MockExamReportMapper;
import com.example.be.mapper.mockexam.MockExamRoomMapper;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.diagnostic.DiagnosticAnswerRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.diagnostic.PartDiagnosticResultRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.setting.ToeicScoreConversionRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.exam.util.QuestionBatchLoader;
import com.example.be.service.mockexam.service.UserMockExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai toàn bộ nghiệp vụ Phòng thi thử TOEIC 200 câu chuẩn ETS (UC17)
 * và Báo cáo nghiệm thu năng lực đầu ra Trước vs Sau (UC18).
 * Tuân thủ chặt chẽ các Business Rules: BR07, BR08/BR13, BR12, BR14, BR15.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMockExamServiceImpl implements UserMockExamService {

    private static final int DEFAULT_MOCK_DURATION_MINUTES = 120;
    private static final int LISTENING_PART_MAX = 4;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final String OUTPUT_LOCKED_REASON =
            "Bạn chưa hoàn thành xong lộ trình học nên chưa thể truy cập đề thi đầu ra";
    private static final List<EAttemptStatus> COMPLETED_STATUSES = List.of(
            EAttemptStatus.SUBMITTED,
            EAttemptStatus.TIMED_OUT
    );
    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private final DiagnosticTestRepository diagnosticTestRepo;
    private final DiagnosticAttemptRepository diagnosticAttemptRepo;
    private final DiagnosticTestQuestionRepository diagnosticTestQuestionRepo;
    private final DiagnosticAnswerRepository diagnosticAnswerRepo;
    private final PartDiagnosticResultRepository partDiagnosticResultRepo;
    private final QuestionRepository questionRepo;
    private final UserRepository userRepo;
    private final TargetProfileRepository targetProfileRepo;
    private final PartRepository partRepo;
    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final ToeicScoreConversionRepository toeicScoreConversionRepo;
    private final LearningPathRepository learningPathRepo;
    private final LearningPathItemRepository learningPathItemRepo;
    private final QuestionBatchLoader questionBatchLoader;
    private final ExamQuestionMapper examQuestionMapper;
    private final MockExamRoomMapper mockExamRoomMapper;
    private final MockExamReportMapper mockExamReportMapper;

    @Override
    @Transactional(readOnly = true)
    public MockExamListResponse listAvailableMockExams(Long userId) {
        // Bước 1: Kiểm tra trạng thái hoàn thành lộ trình để mở khóa đề thi đầu ra (OUTPUT)
        User user = findUserWithTargetOrThrow(userId);
        Integer userTargetScore = user.getTargetProfile() != null ? user.getTargetProfile().getTargetTotalScore() : null;
        boolean isOutputUnlocked = isOutputExamUnlocked(userId);

        // Bước 2: Tải tất cả đề FULL_MOCK_TEST đang ACTIVE trong hệ thống
        List<DiagnosticTest> activeMockTests = diagnosticTestRepo.findActiveByTestTypeOrdered(
                ETestType.FULL_MOCK_TEST,
                EExamStatus.ACTIVE
        );

        // Bước 3: Nạp lịch sử lượt thi thử của học viên và đếm số câu từng đề bằng batch query (tránh N+1)
        List<DiagnosticAttempt> userMockAttempts = diagnosticAttemptRepo
                .findAllByUserIdAndTestTypeWithContextOrderByRecent(userId, ETestType.FULL_MOCK_TEST);

        DiagnosticAttempt activeAttempt = userMockAttempts.stream()
                .filter(a -> a.getStatus() == EAttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        Set<Long> testIdsToCount = activeMockTests.stream()
                .map(DiagnosticTest::getId)
                .collect(Collectors.toSet());
        for (DiagnosticAttempt attempt : userMockAttempts) {
            if (attempt.getDiagnosticTest() != null) {
                testIdsToCount.add(attempt.getDiagnosticTest().getId());
            }
        }
        Map<Long, SectionCounts> testQuestionCounts = loadTestQuestionCounts(testIdsToCount);

        List<DiagnosticAttempt> completedAttempts = userMockAttempts.stream()
                .filter(a -> COMPLETED_STATUSES.contains(a.getStatus()))
                .toList();

        Map<Long, SectionCounts> correctCountsByAttemptId = loadAttemptCorrectCounts(completedAttempts);
        Map<Integer, ToeicScoreConversion> conversionMap = loadToeicConversionMap();

        // Bước 4: Tính thống kê số lần làm & điểm cao nhất theo từng đề, đồng thời dựng danh sách lịch sử
        Map<Long, Integer> attemptCountByTestId = new HashMap<>();
        Map<Long, Integer> bestScoreByTestId = new HashMap<>();
        List<MockExamHistoryItemResponse> historyItems = new ArrayList<>(completedAttempts.size());

        for (DiagnosticAttempt attempt : completedAttempts) {
            if (attempt.getDiagnosticTest() == null) {
                continue;
            }
            Long testId = attempt.getDiagnosticTest().getId();
            SectionCounts totals = testQuestionCounts.getOrDefault(testId, new SectionCounts());
            SectionCounts corrects = correctCountsByAttemptId.getOrDefault(attempt.getId(), new SectionCounts());

            int listeningScore = mockExamReportMapper.resolveSectionToeicScore(
                    corrects.listening, totals.listening, true, conversionMap
            );
            int readingScore = mockExamReportMapper.resolveSectionToeicScore(
                    corrects.reading, totals.reading, false, conversionMap
            );
            int totalScore = listeningScore + readingScore;

            attemptCountByTestId.merge(testId, 1, Integer::sum);
            bestScoreByTestId.merge(testId, totalScore, Math::max);

            Integer attemptAim = attempt.getTargetProfile() != null
                    ? attempt.getTargetProfile().getTargetTotalScore()
                    : userTargetScore;

            historyItems.add(mockExamRoomMapper.toHistoryItemResponse(
                    attempt,
                    listeningScore,
                    readingScore,
                    corrects.total,
                    totals.total,
                    attemptAim
            ));
        }

        // Bước 5: Ánh xạ danh sách đề thi thử kèm cờ khóa đề đầu ra & phiên đang làm dở
        List<MockExamListItemResponse> examItems = new ArrayList<>(activeMockTests.size());
        for (DiagnosticTest test : activeMockTests) {
            boolean isOutput = test.getExamTag() == EExamTag.OUTPUT;
            boolean isLocked = isOutput && !isOutputUnlocked;
            int totalQuestions = testQuestionCounts.getOrDefault(test.getId(), new SectionCounts()).total;

            DiagnosticAttempt inProgressForTest = (activeAttempt != null
                    && activeAttempt.getDiagnosticTest() != null
                    && Objects.equals(activeAttempt.getDiagnosticTest().getId(), test.getId()))
                    ? activeAttempt
                    : null;

            examItems.add(mockExamRoomMapper.toListItemResponse(
                    test,
                    totalQuestions,
                    isLocked,
                    OUTPUT_LOCKED_REASON,
                    inProgressForTest,
                    attemptCountByTestId.getOrDefault(test.getId(), 0),
                    bestScoreByTestId.get(test.getId())
            ));
        }

        return mockExamRoomMapper.toListResponse(
                isOutputUnlocked,
                OUTPUT_LOCKED_REASON,
                activeAttempt,
                userTargetScore,
                examItems,
                historyItems
        );
    }

    @Override
    @Transactional
    public StartDiagnosticResponse startMockExam(Long userId, StartMockExamRequest request) {
        // Bước 1: Kiểm tra tồn tại học viên và đề thi FULL_MOCK_TEST đang ACTIVE (UC17 Ngoại lệ 3a)
        User user = findUserWithTargetOrThrow(userId);
        DiagnosticTest test = diagnosticTestRepo.findById(request.getDiagnosticTestId())
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_EXAM_NOT_ACTIVE));

        if (test.getTestType() != ETestType.FULL_MOCK_TEST || test.getExamStatus() != EExamStatus.ACTIVE) {
            throw new BaseException(ErrorCode.MOCK_EXAM_NOT_ACTIVE);
        }

        // Bước 2: Nếu là đề thi đầu ra (OUTPUT), kiểm tra đã hoàn thành lộ trình học chưa (UC17 Luồng 2a)
        if (test.getExamTag() == EExamTag.OUTPUT && !isOutputExamUnlocked(userId)) {
            throw new BaseException(ErrorCode.MOCK_EXAM_OUTPUT_LOCKED);
        }

        TargetProfile targetProfile = resolveTargetProfile(user, request.getTargetProfileId());
        Long targetProfileId = targetProfile != null ? targetProfile.getId() : null;

        // Bước 3: Kiểm tra phiên IN_PROGRESS hiện có (tái sử dụng nếu cùng đề và còn thời gian; thu bài nếu hết giờ; hủy nếu chuyển sang đề khác)
        Optional<DiagnosticAttempt> activeOpt = diagnosticAttemptRepo
                .findTopByUserIdAndDiagnosticTestTestTypeAndStatusOrderByStartedAtDesc(
                        userId,
                        ETestType.FULL_MOCK_TEST,
                        EAttemptStatus.IN_PROGRESS
                );
        if (activeOpt.isPresent()) {
            DiagnosticAttempt existing = activeOpt.get();
            long remaining = resolveEffectiveRemainingSeconds(existing, existing.getDiagnosticTest());
            if (remaining <= 0) {
                finalizeTimedOutAttempt(existing);
            } else if (existing.getDiagnosticTest() != null
                    && Objects.equals(existing.getDiagnosticTest().getId(), test.getId())) {
                return mockExamRoomMapper.toStartResponse(existing, test, targetProfileId);
            } else {
                existing.setStatus(EAttemptStatus.ABANDONED);
                diagnosticAttemptRepo.save(existing);
            }
        }

        // Bước 4: Khởi tạo phiên thi thử mới ở trạng thái IN_PROGRESS kèm đồng hồ 120 phút (hoặc theo cấu hình đề)
        int initialSeconds = resolveTotalDurationSeconds(test);
        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setUser(user);
        attempt.setDiagnosticTest(test);
        attempt.setTargetProfile(targetProfile);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setRemainingSeconds(initialSeconds);
        attempt = diagnosticAttemptRepo.save(attempt);

        log.info("[UC17] Khởi tạo phiên thi thử attemptId={} cho userId={}, testId={}, examTag={}",
                attempt.getId(), userId, test.getId(), test.getExamTag());

        return mockExamRoomMapper.toStartResponse(attempt, test, targetProfileId);
    }

    @Override
    @Transactional
    public MockExamRoomResponse getActiveSession(Long userId) {
        Optional<DiagnosticAttempt> activeOpt = diagnosticAttemptRepo
                .findTopByUserIdAndDiagnosticTestTestTypeAndStatusOrderByStartedAtDesc(
                        userId,
                        ETestType.FULL_MOCK_TEST,
                        EAttemptStatus.IN_PROGRESS
                );
        if (activeOpt.isEmpty()) {
            return null;
        }

        DiagnosticAttempt attempt = activeOpt.get();
        DiagnosticTest test = attempt.getDiagnosticTest();
        long remaining = resolveEffectiveRemainingSeconds(attempt, test);
        if (remaining <= 0) {
            finalizeTimedOutAttempt(attempt);
            return null;
        }

        return buildRoomResponse(attempt, remaining);
    }

    @Override
    @Transactional(readOnly = true)
    public MockExamRoomResponse getExamRoom(Long userId, Long attemptId) {
        DiagnosticAttempt attempt = loadOwnedMockAttemptOrThrow(userId, attemptId);
        if (attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.MOCK_EXAM_ALREADY_SUBMITTED);
        }

        long remaining = resolveEffectiveRemainingSeconds(attempt, attempt.getDiagnosticTest());
        return buildRoomResponse(attempt, remaining);
    }

    @Override
    @Transactional
    public void saveDraftAnswers(Long userId, SaveMockExamDraftRequest request) {
        // Bước 1: Kiểm tra quyền sở hữu lượt thi và bảo đảm phiên thi vẫn đang mở
        DiagnosticAttempt attempt = loadOwnedMockAttemptOrThrow(userId, request.getAttemptId());
        if (attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.MOCK_EXAM_ALREADY_SUBMITTED);
        }

        DiagnosticTest test = attempt.getDiagnosticTest();
        List<DiagnosticTestQuestion> orderedQuestions = loadOrderedTestQuestions(test);
        Map<Long, Question> questionMap = orderedQuestions.stream()
                .map(DiagnosticTestQuestion::getQuestion)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        // Bước 2: Tải sẵn các bản ghi nháp đã có trên DB để cập nhật tại chỗ (tránh vi phạm UNIQUE uk_diagnostic_answer)
        Map<Long, DiagnosticAnswer> existingByQuestionId = indexAttemptAnswersByQuestionId(attempt.getId());

        List<DiagnosticAnswer> toSave = new ArrayList<>();
        for (SaveMockExamDraftRequest.DraftAnswerItem item : request.getAnswers()) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }

            String normalizedOption = examQuestionMapper.normalizeOptionKey(item.getSelectedOption());
            String correctKey = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
            boolean isCorrect = normalizedOption != null && normalizedOption.equalsIgnoreCase(correctKey);

            DiagnosticAnswer answer = existingByQuestionId.getOrDefault(question.getId(), new DiagnosticAnswer());
            answer.setDiagnosticAttempt(attempt);
            answer.setQuestion(question);
            answer.setUserAnswer(buildUserAnswerJson(normalizedOption));
            answer.setIsCorrect(isCorrect);
            answer.setIsFlagged(Boolean.TRUE.equals(item.getIsFlagged()));
            toSave.add(answer);
        }

        if (!toSave.isEmpty()) {
            diagnosticAnswerRepo.saveAll(toSave);
        }
    }

    @Override
    @Transactional
    public long syncRemainingTime(Long userId, SyncMockExamTimerRequest request) {
        DiagnosticAttempt attempt = loadOwnedMockAttemptOrThrow(userId, request.getAttemptId());
        if (attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.MOCK_EXAM_ALREADY_SUBMITTED);
        }

        int totalAllowed = resolveTotalDurationSeconds(attempt.getDiagnosticTest());
        int currentStored = attempt.getRemainingSeconds() != null
                ? Math.min(attempt.getRemainingSeconds(), totalAllowed)
                : totalAllowed;
        int requested = Math.max(0, request.getRemainingSeconds());
        // Clamp thời gian để ngăn client tự tăng thêm giờ làm bài
        int clamped = Math.min(currentStored, requested);

        attempt.setRemainingSeconds(clamped);
        diagnosticAttemptRepo.save(attempt);
        return clamped;
    }

    @Override
    @Transactional
    public MockExamSubmitResponse submitMockExam(Long userId, SubmitMockExamRequest request) {
        DiagnosticAttempt attempt = loadOwnedMockAttemptOrThrow(userId, request.getAttemptId());
        if (attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.MOCK_EXAM_ALREADY_SUBMITTED);
        }

        if (request.getTargetProfileId() != null) {
            TargetProfile resolved = resolveTargetProfile(attempt.getUser(), request.getTargetProfileId());
            attempt.setTargetProfile(resolved);
        }

        return gradeAndFinalizeAttempt(attempt, request);
    }

    @Override
    @Transactional(readOnly = true)
    public MockExamReportResponse getReport(Long userId, Long attemptId) {
        // Bước 1: Tải lượt thi thử đã hoàn thành và kiểm tra chống IDOR
        DiagnosticAttempt mockAttempt = loadCompletedMockAttemptOrThrow(userId, attemptId);
        DiagnosticTest test = mockAttempt.getDiagnosticTest();
        EExamTag examTag = test != null && test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE;

        // Bước 2: Tải kết quả 7 Part, câu trả lời và câu hỏi kèm Abilities
        List<PartDiagnosticResult> afterPartResults = partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(attemptId);
        List<DiagnosticAnswer> mockAnswers = diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(attemptId);
        List<Question> examQuestions = loadOrderedExamQuestionsWithAbilities(test);
        Map<Integer, ToeicScoreConversion> conversionMap = loadToeicConversionMap();

        TargetProfile targetProfile = resolveEffectiveTargetProfile(userId, mockAttempt);

        // Bước 3: Chỉ tải dữ liệu đối sánh Trước vs Sau & quy tắc Ability khi đây là đề thi đầu ra (OUTPUT)
        List<PartDiagnosticResult> beforePartResults = Collections.emptyList();
        List<AbilityEvaluationRule> abilityRules = Collections.emptyList();
        Map<Long, TargetPartThreshold> thresholdByPartId = Collections.emptyMap();

        if (examTag == EExamTag.OUTPUT) {
            beforePartResults = loadBeforeDiagnosticPartResults(userId);
            abilityRules = abilityRuleRepo.findAllWithDetails();
            if (targetProfile != null) {
                thresholdByPartId = targetPartThresholdRepo.findByTargetProfileId(targetProfile.getId())
                        .stream()
                        .filter(t -> t.getPart() != null)
                        .collect(Collectors.toMap(t -> t.getPart().getId(), t -> t, (a, b) -> a));
            }
        }

        return mockExamReportMapper.toReportResponse(
                mockAttempt,
                afterPartResults,
                mockAnswers,
                examQuestions,
                beforePartResults,
                abilityRules,
                thresholdByPartId,
                conversionMap,
                targetProfile
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MockExamReportResponse getLatestOutputReport(Long userId) {
        List<DiagnosticAttempt> completedMockAttempts = diagnosticAttemptRepo
                .findAllByUserIdAndTestTypeWithContextOrderByRecent(userId, ETestType.FULL_MOCK_TEST)
                .stream()
                .filter(a -> COMPLETED_STATUSES.contains(a.getStatus()))
                .toList();

        DiagnosticAttempt latestOutput = completedMockAttempts.stream()
                .filter(a -> a.getDiagnosticTest() != null && a.getDiagnosticTest().getExamTag() == EExamTag.OUTPUT)
                .findFirst()
                .or(() -> completedMockAttempts.stream().findFirst())
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_EXAM_NOT_COMPLETED));

        return getReport(userId, latestOutput.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public MockExamReviewResponse getReviewData(Long userId, Long attemptId) {
        DiagnosticAttempt attempt = loadCompletedMockAttemptOrThrow(userId, attemptId);
        DiagnosticTest test = attempt.getDiagnosticTest();

        List<DiagnosticTestQuestion> orderedQuestions = loadOrderedTestQuestions(test);
        List<Long> questionIds = orderedQuestions.stream()
                .map(DiagnosticTestQuestion::getQuestion)
                .filter(Objects::nonNull)
                .map(Question::getId)
                .toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(questionIds);
        Map<Long, DiagnosticAnswer> answerByQuestionId = indexAttemptAnswersByQuestionId(attemptId);

        // Chặn xem đáp án và lời giải chi tiết khi bài thi đầu ra (OUTPUT) chưa đạt mục tiêu AIM
        if (test != null && test.getExamTag() == EExamTag.OUTPUT) {
            TargetProfile targetProfile = resolveEffectiveTargetProfile(userId, attempt);
            Integer targetScore = targetProfile != null ? targetProfile.getTargetTotalScore() : null;
            int totalToeicScore = calculateTotalToeicScore(orderedQuestions, detailedQuestionMap, answerByQuestionId);
            if (!MockExamReportMapper.isReviewUnlocked(EExamTag.OUTPUT, totalToeicScore, targetScore)) {
                throw new BaseException(
                        ErrorCode.MOCK_EXAM_REVIEW_LOCKED,
                        String.format(
                                "Bạn đạt %d/990 điểm, chưa đạt mục tiêu AIM %d nên chưa thể xem đáp án và lời giải chi tiết cho bài thi đầu ra.",
                                totalToeicScore,
                                targetScore
                        )
                );
            }
        }

        return mockExamRoomMapper.toReviewResponse(
                test,
                attempt,
                orderedQuestions,
                detailedQuestionMap,
                answerByQuestionId
        );
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private User findUserWithTargetOrThrow(Long userId) {
        return userRepo.findWithTargetProfileById(userId)
                .or(() -> userRepo.findById(userId))
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private TargetProfile resolveTargetProfile(User user, Long requestedTargetProfileId) {
        if (requestedTargetProfileId != null) {
            if (user.getTargetProfile() != null && requestedTargetProfileId.equals(user.getTargetProfile().getId())) {
                return user.getTargetProfile();
            }
            TargetProfile profile = targetProfileRepo.findById(requestedTargetProfileId)
                    .orElseThrow(() -> new BaseException(ErrorCode.TARGET_NOT_FOUND));
            if (!Boolean.TRUE.equals(profile.getStatus())) {
                throw new BaseException(ErrorCode.TARGET_PROFILE_INACTIVE);
            }
            return profile;
        }
        return user.getTargetProfile();
    }

    private Optional<LearningPath> resolveCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepo
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(lp -> lp.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> Optional.of(candidates.get(0)));
    }

    private boolean isOutputExamUnlocked(Long userId) {
        Optional<LearningPath> pathOpt = resolveCurrentLearningPath(userId);
        if (pathOpt.isEmpty()) {
            return false;
        }
        LearningPath currentPath = pathOpt.get();
        if (currentPath.getStatus() == ELearningPathStatus.COMPLETED) {
            return true;
        }
        List<LearningPathItem> items = learningPathItemRepo
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(currentPath.getId());
        return !items.isEmpty() && items.stream().allMatch(item -> item.getStatus() == ELessonStatus.COMPLETED);
    }

    private DiagnosticAttempt loadOwnedMockAttemptOrThrow(Long userId, Long attemptId) {
        DiagnosticAttempt attempt = diagnosticAttemptRepo.findByIdWithContext(attemptId)
                .or(() -> diagnosticAttemptRepo.findById(attemptId))
                .orElseThrow(() -> new BaseException(ErrorCode.MOCK_EXAM_ATTEMPT_NOT_FOUND));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập lượt thi thử này");
        }
        if (attempt.getDiagnosticTest() == null
                || attempt.getDiagnosticTest().getTestType() != ETestType.FULL_MOCK_TEST) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Lượt thi này không phải bài thi thử ETS");
        }
        return attempt;
    }

    private DiagnosticAttempt loadCompletedMockAttemptOrThrow(Long userId, Long attemptId) {
        DiagnosticAttempt attempt = loadOwnedMockAttemptOrThrow(userId, attemptId);
        if (!COMPLETED_STATUSES.contains(attempt.getStatus())) {
            throw new BaseException(ErrorCode.MOCK_EXAM_NOT_COMPLETED);
        }
        return attempt;
    }

    private MockExamRoomResponse buildRoomResponse(DiagnosticAttempt attempt, long remainingSeconds) {
        DiagnosticTest test = attempt.getDiagnosticTest();
        List<DiagnosticTestQuestion> orderedQuestions = loadOrderedTestQuestions(test);
        List<Long> questionIds = orderedQuestions.stream()
                .map(tq -> tq.getQuestion().getId())
                .toList();

        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(questionIds);
        Map<Long, DiagnosticAnswer> draftByQuestionId = indexAttemptAnswersByQuestionId(attempt.getId());

        return mockExamRoomMapper.toRoomResponse(
                test,
                attempt,
                orderedQuestions,
                detailedQuestionMap,
                draftByQuestionId,
                remainingSeconds
        );
    }

    private void finalizeTimedOutAttempt(DiagnosticAttempt expiredAttempt) {
        SubmitMockExamRequest autoRequest = new SubmitMockExamRequest();
        autoRequest.setAttemptId(expiredAttempt.getId());
        autoRequest.setDurationSeconds(resolveTotalDurationSeconds(expiredAttempt.getDiagnosticTest()));
        autoRequest.setIsTimedOut(true);
        autoRequest.setAnswers(Collections.emptyList());
        gradeAndFinalizeAttempt(expiredAttempt, autoRequest);
    }

    private MockExamSubmitResponse gradeAndFinalizeAttempt(
            DiagnosticAttempt attempt,
            SubmitMockExamRequest request
    ) {
        DiagnosticTest test = attempt.getDiagnosticTest();
        List<DiagnosticTestQuestion> orderedQuestions = loadOrderedTestQuestions(test);
        List<Long> questionIds = orderedQuestions.stream()
                .map(tq -> tq.getQuestion().getId())
                .toList();

        Map<Long, Question> questionById = questionRepo.findAllWithAbilitiesByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        Map<Long, DiagnosticAnswer> existingByQuestionId = indexAttemptAnswersByQuestionId(attempt.getId());

        // Khử trùng lặp questionId từ client để chống gửi lặp câu đúng nhằm gian lận điểm
        Map<Long, SubmitMockExamRequest.AnswerItem> submittedByQuestionId = new HashMap<>();
        if (request.getAnswers() != null) {
            for (SubmitMockExamRequest.AnswerItem item : request.getAnswers()) {
                if (item != null && item.getQuestionId() != null) {
                    submittedByQuestionId.putIfAbsent(item.getQuestionId(), item);
                }
            }
        }

        List<DiagnosticAnswer> answersToSave = new ArrayList<>(questionIds.size());
        int listeningTotal = 0;
        int listeningCorrect = 0;
        int readingTotal = 0;
        int readingCorrect = 0;

        Map<Long, List<Question>> questionsByPartId = new HashMap<>();

        for (DiagnosticTestQuestion tq : orderedQuestions) {
            Question question = questionById.getOrDefault(tq.getQuestion().getId(), tq.getQuestion());
            if (question == null) {
                continue;
            }
            if (question.getPart() != null) {
                questionsByPartId.computeIfAbsent(question.getPart().getId(), k -> new ArrayList<>()).add(question);
            }

            DiagnosticAnswer answer = existingByQuestionId.getOrDefault(question.getId(), new DiagnosticAnswer());
            SubmitMockExamRequest.AnswerItem submitted = submittedByQuestionId.get(question.getId());

            String selectedKey;
            boolean isFlagged;
            if (submitted != null) {
                selectedKey = examQuestionMapper.extractAnswerKey(submitted.getUserAnswer());
                isFlagged = Boolean.TRUE.equals(submitted.getIsFlagged());
            } else {
                // Giữ lại đáp án nháp đã lưu trước đó trên DB (phục vụ auto-submit khi hết giờ BR07)
                selectedKey = examQuestionMapper.extractAnswerKey(answer.getUserAnswer());
                isFlagged = Boolean.TRUE.equals(answer.getIsFlagged());
            }

            String correctKey = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
            boolean isCorrect = selectedKey != null && selectedKey.equalsIgnoreCase(correctKey);

            answer.setDiagnosticAttempt(attempt);
            answer.setQuestion(question);
            answer.setUserAnswer(buildUserAnswerJson(selectedKey));
            answer.setIsCorrect(isCorrect);
            answer.setIsFlagged(isFlagged);
            answersToSave.add(answer);

            int partNo = examQuestionMapper.resolvePartNumber(question.getPart());
            if (partNo <= 4) {
                listeningTotal++;
                if (isCorrect) {
                    listeningCorrect++;
                }
            } else {
                readingTotal++;
                if (isCorrect) {
                    readingCorrect++;
                }
            }
        }

        diagnosticAnswerRepo.saveAll(answersToSave);

        // Chốt kết quả từng Part (PartDiagnosticResult) cho lượt thi thử này mà không ghi đè UserAbility/LearningPath
        TargetProfile targetProfile = resolveEffectiveTargetProfile(attempt.getUser().getId(), attempt);
        Long targetProfileId = targetProfile != null ? targetProfile.getId() : null;
        Map<Long, TargetPartThreshold> thresholdByPartId = targetProfileId != null
                ? targetPartThresholdRepo.findByTargetProfileId(targetProfileId).stream()
                        .filter(t -> t.getPart() != null)
                        .collect(Collectors.toMap(t -> t.getPart().getId(), t -> t, (a, b) -> a))
                : Collections.emptyMap();

        Map<Long, DiagnosticAnswer> savedAnswerByQuestionId = answersToSave.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<PartDiagnosticResult> partResults = new ArrayList<>();
        for (Map.Entry<Long, List<Question>> entry : questionsByPartId.entrySet()) {
            Long partId = entry.getKey();
            List<Question> partQuestions = entry.getValue();
            int total = partQuestions.size();
            int correct = (int) partQuestions.stream()
                    .filter(q -> {
                        DiagnosticAnswer ans = savedAnswerByQuestionId.get(q.getId());
                        return ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
                    })
                    .count();
            float score = total > 0 ? (correct * 100.0f) / total : 0.0f;

            TargetPartThreshold threshold = thresholdByPartId.get(partId);
            EPartDirective directive = threshold != null
                    ? threshold.determineDirective(score)
                    : EPartDirective.ABILITY_DIAGNOSTIC;

            PartDiagnosticResult pr = new PartDiagnosticResult();
            pr.setDiagnosticAttempt(attempt);
            pr.setPart(partRepo.getReferenceById(partId));
            pr.setScore(score);
            pr.setDirective(directive);
            partResults.add(pr);
        }
        partDiagnosticResultRepo.saveAll(partResults);

        int maxAllowedSeconds = resolveTotalDurationSeconds(test);
        int safeDuration = request.getDurationSeconds() != null
                ? Math.max(1, Math.min(maxAllowedSeconds, request.getDurationSeconds()))
                : maxAllowedSeconds;

        attempt.setStatus(Boolean.TRUE.equals(request.getIsTimedOut())
                ? EAttemptStatus.TIMED_OUT
                : EAttemptStatus.SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setDurationSeconds(safeDuration);
        attempt.setRemainingSeconds(0);
        diagnosticAttemptRepo.save(attempt);

        Map<Integer, ToeicScoreConversion> conversionMap = loadToeicConversionMap();
        int listeningScore = mockExamReportMapper.resolveSectionToeicScore(
                listeningCorrect, listeningTotal, true, conversionMap
        );
        int readingScore = mockExamReportMapper.resolveSectionToeicScore(
                readingCorrect, readingTotal, false, conversionMap
        );
        int totalCorrect = listeningCorrect + readingCorrect;
        int totalQuestions = listeningTotal + readingTotal;
        Integer targetScore = targetProfile != null ? targetProfile.getTargetTotalScore() : null;

        log.info("[UC17] Hoàn tất chấm điểm bài thi thử attemptId={}, userId={}, tag={}, score={}/990",
                attempt.getId(), attempt.getUser().getId(), test.getExamTag(), listeningScore + readingScore);

        return mockExamReportMapper.toSubmitResponse(
                attempt,
                test,
                listeningScore,
                readingScore,
                totalCorrect,
                totalQuestions,
                targetScore
        );
    }

    private List<PartDiagnosticResult> loadBeforeDiagnosticPartResults(Long userId) {
        Optional<LearningPath> pathOpt = resolveCurrentLearningPath(userId);
        if (pathOpt.isPresent() && pathOpt.get().getDiagnosticAttempt() != null) {
            return partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(
                    pathOpt.get().getDiagnosticAttempt().getId()
            );
        }

        Optional<DiagnosticAttempt> latestDiagnosticOpt = diagnosticAttemptRepo
                .findTopByUserIdAndDiagnosticTestTestTypeAndStatusInOrderByCompletedAtDesc(
                        userId,
                        ETestType.DIAGNOSTIC,
                        COMPLETED_STATUSES
                );
        return latestDiagnosticOpt
                .map(da -> partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(da.getId()))
                .orElse(Collections.emptyList());
    }

    private TargetProfile resolveEffectiveTargetProfile(Long userId, DiagnosticAttempt mockAttempt) {
        if (mockAttempt.getTargetProfile() != null) {
            return mockAttempt.getTargetProfile();
        }
        Optional<LearningPath> pathOpt = resolveCurrentLearningPath(userId);
        if (pathOpt.isPresent() && pathOpt.get().getTargetProfile() != null) {
            return pathOpt.get().getTargetProfile();
        }
        return mockAttempt.getUser() != null ? mockAttempt.getUser().getTargetProfile() : null;
    }

    private List<DiagnosticTestQuestion> loadOrderedTestQuestions(DiagnosticTest test) {
        if (test == null) {
            return Collections.emptyList();
        }
        List<DiagnosticTestQuestion> ordered = diagnosticTestQuestionRepo
                .findAllByDiagnosticTestIdOrderByOrderNoAsc(test.getId());
        if (ordered.isEmpty() && test.getTestQuestions() != null && !test.getTestQuestions().isEmpty()) {
            ordered = test.getTestQuestions();
        }
        return ordered;
    }

    private List<Question> loadOrderedExamQuestionsWithAbilities(DiagnosticTest test) {
        List<DiagnosticTestQuestion> orderedLinks = loadOrderedTestQuestions(test);
        if (orderedLinks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderedIds = orderedLinks.stream()
                .map(tq -> tq.getQuestion().getId())
                .distinct()
                .toList();

        Map<Long, Question> questionById = questionRepo.findAllWithAbilitiesByIdIn(orderedIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        List<Question> orderedQuestions = new ArrayList<>(orderedIds.size());
        for (Long qId : orderedIds) {
            Question loaded = questionById.get(qId);
            if (loaded != null) {
                orderedQuestions.add(loaded);
            }
        }
        return orderedQuestions;
    }

    private Map<Long, DiagnosticAnswer> indexAttemptAnswersByQuestionId(Long attemptId) {
        return diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(attemptId).stream()
                .filter(ans -> ans.getQuestion() != null)
                .collect(Collectors.toMap(
                        ans -> ans.getQuestion().getId(),
                        ans -> ans,
                        (first, second) -> first
                ));
    }

    private Map<Long, SectionCounts> loadTestQuestionCounts(Set<Long> testIds) {
        if (testIds == null || testIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SectionCounts> map = new HashMap<>();
        List<Object[]> rows = diagnosticTestQuestionRepo.countQuestionsByTestIdsAndSection(testIds);
        for (Object[] row : rows) {
            Long testId = ((Number) row[0]).longValue();
            ESection section = (ESection) row[1];
            String partName = (String) row[2];
            int count = ((Number) row[3]).intValue();

            SectionCounts counts = map.computeIfAbsent(testId, k -> new SectionCounts());
            counts.total += count;
            if (isListeningSection(section, partName)) {
                counts.listening += count;
            } else {
                counts.reading += count;
            }
        }
        return map;
    }

    private Map<Long, SectionCounts> loadAttemptCorrectCounts(List<DiagnosticAttempt> completedAttempts) {
        if (completedAttempts == null || completedAttempts.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> attemptIds = completedAttempts.stream()
                .map(DiagnosticAttempt::getId)
                .toList();
        Map<Long, SectionCounts> map = new HashMap<>();
        List<Object[]> rows = diagnosticAnswerRepo.findAnswerStatsByAttemptIds(attemptIds);
        for (Object[] row : rows) {
            Long attemptId = ((Number) row[0]).longValue();
            ESection section = (ESection) row[1];
            String partName = (String) row[2];
            boolean isCorrect = Boolean.TRUE.equals(row[3]);
            if (!isCorrect) {
                continue;
            }

            SectionCounts counts = map.computeIfAbsent(attemptId, k -> new SectionCounts());
            counts.total++;
            if (isListeningSection(section, partName)) {
                counts.listening++;
            } else {
                counts.reading++;
            }
        }
        return map;
    }

    private int calculateTotalToeicScore(
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, DiagnosticAnswer> answerByQuestionId
    ) {
        int listeningTotal = 0;
        int listeningCorrect = 0;
        int readingTotal = 0;
        int readingCorrect = 0;

        for (DiagnosticTestQuestion tq : orderedQuestions) {
            if (tq.getQuestion() == null) {
                continue;
            }
            Question question = detailedQuestionMap.getOrDefault(tq.getQuestion().getId(), tq.getQuestion());
            int partNo = examQuestionMapper.resolvePartNumber(question.getPart());
            DiagnosticAnswer answer = answerByQuestionId.get(question.getId());
            boolean isCorrect = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());

            if (partNo <= LISTENING_PART_MAX) {
                listeningTotal++;
                if (isCorrect) {
                    listeningCorrect++;
                }
            } else {
                readingTotal++;
                if (isCorrect) {
                    readingCorrect++;
                }
            }
        }

        Map<Integer, ToeicScoreConversion> conversionMap = loadToeicConversionMap();
        int listeningScore = mockExamReportMapper.resolveSectionToeicScore(
                listeningCorrect, listeningTotal, true, conversionMap
        );
        int readingScore = mockExamReportMapper.resolveSectionToeicScore(
                readingCorrect, readingTotal, false, conversionMap
        );
        return listeningScore + readingScore;
    }

    private Map<Integer, ToeicScoreConversion> loadToeicConversionMap() {
        return toeicScoreConversionRepo.findAll().stream()
                .collect(Collectors.toMap(ToeicScoreConversion::getCorrectCount, c -> c, (a, b) -> a));
    }

    private boolean isListeningSection(ESection section, String partName) {
        if (section != null) {
            return section == ESection.LISTENING;
        }
        if (partName != null) {
            Part fakePart = new Part();
            fakePart.setName(partName);
            return examQuestionMapper.resolvePartNumber(fakePart) <= 4;
        }
        return true;
    }

    private int resolveTotalDurationSeconds(DiagnosticTest test) {
        int durationMinutes = (test != null && test.getDurationMinutes() != null && test.getDurationMinutes() > 0)
                ? test.getDurationMinutes()
                : DEFAULT_MOCK_DURATION_MINUTES;
        return durationMinutes * (int) SECONDS_PER_MINUTE;
    }

    private long resolveEffectiveRemainingSeconds(DiagnosticAttempt attempt, DiagnosticTest test) {
        int totalAllowed = resolveTotalDurationSeconds(test);
        if (attempt.getRemainingSeconds() != null) {
            return Math.max(0L, Math.min(totalAllowed, attempt.getRemainingSeconds()));
        }
        long elapsed = attempt.getStartedAt() != null
                ? Math.max(0L, ChronoUnit.SECONDS.between(attempt.getStartedAt(), LocalDateTime.now()))
                : 0L;
        return Math.max(0L, totalAllowed - elapsed);
    }

    private String buildUserAnswerJson(String normalizedOption) {
        if (normalizedOption == null) {
            return null;
        }
        return "{\"selected\":\"" + normalizedOption + "\"}";
    }

    private static class SectionCounts {
        int total = 0;
        int listening = 0;
        int reading = 0;
    }
}
