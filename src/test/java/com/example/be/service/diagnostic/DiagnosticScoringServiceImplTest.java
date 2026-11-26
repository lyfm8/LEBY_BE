package com.example.be.service.diagnostic;

import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SyncDiagnosticTimerRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.dto.response.diagnostic.PartResultResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.diagnostic.DiagnosticResultMapper;
import com.example.be.mapper.diagnostic.DiagnosticRoomMapper;
import com.example.be.repository.ability.UserAbilityRepository;
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
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.content.AdaptivePathGeneratorService;
import com.example.be.service.diagnostic.implement.DiagnosticScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho DiagnosticScoringServiceImpl.
 * Kiểm tra luồng mở phòng thi (UC06), resume, BR02, chấm điểm 2 tầng,
 * phân loại UserAbility và xuất báo cáo UC07.
 */
@ExtendWith(MockitoExtension.class)
class DiagnosticScoringServiceImplTest {

    @Mock private DiagnosticTestRepository diagnosticTestRepo;
    @Mock private DiagnosticAttemptRepository diagnosticAttemptRepo;
    @Mock private DiagnosticTestQuestionRepository diagnosticTestQuestionRepo;
    @Mock private PartDiagnosticResultRepository partDiagnosticResultRepo;
    @Mock private DiagnosticAnswerRepository diagnosticAnswerRepo;
    @Mock private QuestionRepository questionRepo;
    @Mock private TargetPartThresholdRepository targetPartThresholdRepo;
    @Mock private AbilityEvaluationRuleRepository abilityRuleRepo;
    @Mock private UserAbilityRepository userAbilityRepo;
    @Mock private UserRepository userRepo;
    @Mock private TargetProfileRepository targetProfileRepo;
    @Mock private PartRepository partRepo;
    @Mock private LearningPathRepository learningPathRepo;
    @Mock private AdaptivePathGeneratorService adaptivePathGeneratorService;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Spy private DiagnosticRoomMapper diagnosticRoomMapper = new DiagnosticRoomMapper(new ObjectMapper());
    @Spy private DiagnosticResultMapper diagnosticResultMapper = new DiagnosticResultMapper();

    @InjectMocks
    private DiagnosticScoringServiceImpl service;

    @Captor
    private ArgumentCaptor<List<UserAbility>> userAbilitiesCaptor;

    @Captor
    private ArgumentCaptor<DiagnosticAttempt> attemptCaptor;

    private User student;
    private TargetProfile target650;
    private DiagnosticTest diagnosticTest;
    private DiagnosticAttempt attempt;
    private Part part1;
    private Ability ability1;
    private Question question;

    @BeforeEach
    void setUp() {
        target650 = new TargetProfile();
        target650.setId(5L);
        target650.setTargetTotalScore(650);
        target650.setStatus(true);

        student = new User();
        student.setId(1L);
        student.setTargetProfile(target650);

        part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1 - Photographs");

        ability1 = new Ability();
        ability1.setId(10L);
        ability1.setName("Nghe tranh có người");

        diagnosticTest = new DiagnosticTest();
        diagnosticTest.setId(100L);
        diagnosticTest.setTitle("Bài thi chẩn đoán đầu vào");
        diagnosticTest.setTestType(ETestType.DIAGNOSTIC);
        diagnosticTest.setExamStatus(EExamStatus.ACTIVE);
        diagnosticTest.setDurationMinutes(60);
        diagnosticTest.setTestQuestions(Collections.emptyList());

        attempt = new DiagnosticAttempt();
        attempt.setId(200L);
        attempt.setUser(student);
        attempt.setDiagnosticTest(diagnosticTest);
        attempt.setTargetProfile(target650);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));

        QuestionAbility qa = new QuestionAbility();
        qa.setAbility(ability1);

        question = new Question();
        question.setId(1L);
        question.setPart(part1);
        question.setCorrectAnswer("{\"selected\":\"A\"}");
        question.setQuestionAbilities(List.of(qa));
    }

    // =========================================================================
    // TEST startDiagnostic (UC06 + BR02)
    // =========================================================================

    @Test
    void startDiagnostic_success() {
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);

        StartDiagnosticResponse resp = service.startDiagnostic(1L, req);

        assertThat(resp.getAttemptId()).isEqualTo(200L);
        assertThat(resp.getDurationMinutes()).isEqualTo(60);
        assertThat(resp.getTargetProfileId()).isEqualTo(5L);
    }

    @Test
    void startDiagnostic_resolvesActiveDiagnosticTestWhenIdOmitted() {
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findFirstByTestTypeAndExamStatus(ETestType.DIAGNOSTIC, EExamStatus.ACTIVE))
                .thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        StartDiagnosticResponse resp = service.startDiagnostic(1L, new StartDiagnosticRequest());

        assertThat(resp.getDiagnosticTestId()).isEqualTo(100L);
    }

    @Test
    void startDiagnostic_noActiveTest_throwsExamNotActive() {
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findFirstByTestTypeAndExamStatus(ETestType.DIAGNOSTIC, EExamStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startDiagnostic(1L, new StartDiagnosticRequest()))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_NOT_ACTIVE);
    }

    @Test
    void startDiagnostic_wrongTestType_throwsBadRequest() {
        diagnosticTest.setTestType(ETestType.FULL_MOCK_TEST);
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);

        assertThatThrownBy(() -> service.startDiagnostic(1L, req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void startDiagnostic_alreadyCompletedSameAim_throwsBr02Conflict() {
        DiagnosticAttempt submittedForSameAim = new DiagnosticAttempt();
        submittedForSameAim.setId(199L);
        submittedForSameAim.setStatus(EAttemptStatus.SUBMITTED);
        submittedForSameAim.setTargetProfile(target650);

        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.findTopByUserIdAndTargetProfileIdAndStatusOrderByCompletedAtDesc(
                1L, 5L, EAttemptStatus.SUBMITTED))
                .thenReturn(Optional.of(submittedForSameAim));

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);

        assertThatThrownBy(() -> service.startDiagnostic(1L, req))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.DIAGNOSTIC_ALREADY_COMPLETED);
    }

    @Test
    void startDiagnostic_forceRetakeSameAim_createsNewAttemptWithInitialRemainingSeconds() {
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.save(any())).thenAnswer(invocation -> {
            DiagnosticAttempt saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(300L);
            }
            return saved;
        });

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);
        req.setForceRetake(true);

        StartDiagnosticResponse resp = service.startDiagnostic(1L, req);

        assertThat(resp.getAttemptId()).isEqualTo(300L);
        verify(diagnosticAttemptRepo).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getRemainingSeconds()).isEqualTo(3600);
        verify(diagnosticAttemptRepo, never()).findTopByUserIdAndTargetProfileIdAndStatusOrderByCompletedAtDesc(
                anyLong(), anyLong(), any());
    }

    @Test
    void startDiagnostic_changedAim_allowsRetake() {
        TargetProfile target750 = new TargetProfile();
        target750.setId(6L);
        target750.setTargetTotalScore(750);
        target750.setStatus(true);
        student.setTargetProfile(target750);

        DiagnosticAttempt attemptFor750 = new DiagnosticAttempt();
        attemptFor750.setId(205L);
        attemptFor750.setTargetProfile(target750);
        attemptFor750.setStatus(EAttemptStatus.IN_PROGRESS);

        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findFirstByTestTypeAndExamStatus(ETestType.DIAGNOSTIC, EExamStatus.ACTIVE))
                .thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.findTopByUserIdAndTargetProfileIdAndStatusOrderByCompletedAtDesc(
                1L, 6L, EAttemptStatus.SUBMITTED))
                .thenReturn(Optional.empty());
        when(diagnosticAttemptRepo.save(any())).thenReturn(attemptFor750);

        StartDiagnosticResponse resp = service.startDiagnostic(1L, new StartDiagnosticRequest());

        assertThat(resp.getAttemptId()).isEqualTo(205L);
        assertThat(resp.getTargetProfileId()).isEqualTo(6L);
    }

    // =========================================================================
    // TEST getExamForAttempt & getActiveSession
    // =========================================================================

    @Test
    void getExamForAttempt_success_returnsMappedQuestionsAndRemainingTime() {
        question.setQuestionData("{\"text\":\"Look at the picture.\",\"options\":[{\"key\":\"A\",\"text\":\"Man reading\"}]}");
        DiagnosticTestQuestion link = new DiagnosticTestQuestion();
        link.setOrderNo(1);
        link.setQuestion(question);

        when(diagnosticAttemptRepo.findByIdWithContext(200L)).thenReturn(Optional.of(attempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(100L))
                .thenReturn(List.of(link));
        when(questionRepo.findAllWithMediasAndParentByIdIn(List.of(1L))).thenReturn(List.of(question));

        DiagnosticRoomResponse room = service.getExamForAttempt(1L, 200L);

        assertThat(room.getAttemptId()).isEqualTo(200L);
        assertThat(room.getQuestions()).hasSize(1);
        assertThat(room.getQuestions().get(0).getTitle()).isEqualTo("Look at the picture.");
        assertThat(room.getRemainingSeconds()).isGreaterThan(0L).isLessThanOrEqualTo(3600L);
    }

    @Test
    void getActiveSession_aimChanged_abandonsOldAttemptAndReturnsNull() {
        TargetProfile target750 = new TargetProfile();
        target750.setId(6L);
        target750.setTargetTotalScore(750);
        student.setTargetProfile(target750); // user đã đổi sang 750 nhưng attempt đang IN_PROGRESS cho 650 (id=5)

        when(diagnosticAttemptRepo.findTopByUserIdAndStatusOrderByStartedAtDesc(1L, EAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));

        DiagnosticRoomResponse resp = service.getActiveSession(1L);

        assertThat(resp).isNull();
        verify(diagnosticAttemptRepo).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo(EAttemptStatus.ABANDONED);
    }

    @Test
    void getExamForAttempt_usesFrozenRemainingSecondsWhenPresent() {
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(120)); // vượt quá 60 phút nếu tính theo đồng hồ thực
        attempt.setRemainingSeconds(1920); // nhưng server đã ngưng đọng ở mức 1920s

        DiagnosticTestQuestion link = new DiagnosticTestQuestion();
        link.setOrderNo(1);
        link.setQuestion(question);
        question.setQuestionData("{\"text\":\"Sample\",\"options\":[{\"key\":\"A\",\"text\":\"Opt\"}]}");

        when(diagnosticAttemptRepo.findByIdWithContext(200L)).thenReturn(Optional.of(attempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(100L)).thenReturn(List.of(link));
        when(questionRepo.findAllWithMediasAndParentByIdIn(List.of(1L))).thenReturn(List.of(question));

        DiagnosticRoomResponse room = service.getExamForAttempt(1L, 200L);

        assertThat(room.getRemainingSeconds()).isEqualTo(1920L);
    }

    @Test
    void syncRemainingTime_clampsNeverToExceedStoredSecondsAndBlocksWrongUser() {
        attempt.setRemainingSeconds(2000);
        when(diagnosticAttemptRepo.findByIdWithContext(200L)).thenReturn(Optional.of(attempt));
        when(diagnosticAttemptRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Client cố gửi số giây lớn hơn (3500 > 2000) -> server giữ nguyên 2000
        SyncDiagnosticTimerRequest cheatReq = new SyncDiagnosticTimerRequest();
        cheatReq.setAttemptId(200L);
        cheatReq.setRemainingSeconds(3500);
        assertThat(service.syncRemainingTime(1L, cheatReq)).isEqualTo(2000L);

        // Client gửi số giây giảm hợp lệ (1850 <= 2000) -> cập nhật 1850
        SyncDiagnosticTimerRequest validReq = new SyncDiagnosticTimerRequest();
        validReq.setAttemptId(200L);
        validReq.setRemainingSeconds(1850);
        assertThat(service.syncRemainingTime(1L, validReq)).isEqualTo(1850L);

        // User khác gọi vào attempt của user 1 -> chặn FORBIDDEN (anti-IDOR)
        assertThatThrownBy(() -> service.syncRemainingTime(999L, validReq))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // =========================================================================
    // TEST submitDiagnostic
    // =========================================================================

    @Test
    void submitDiagnostic_matchesAdminKeyFormatAndSavesIsFlagged() {
        // Admin UI lưu {"key":"A"}, FE gửi {"selected":"A"} kèm isFlagged=true
        question.setCorrectAnswer("{\"key\":\"A\"}");
        setupSubmitMocksWithThreshold("{\"selected\":\"A\"}", 80.0f, null, null);

        SubmitDiagnosticRequest.AnswerItem item = buildAnswerItem(1L, "{\"selected\":\"A\"}");
        item.setIsFlagged(true);
        SubmitDiagnosticRequest req = buildSubmitRequestWithItems(List.of(item));

        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(1);
        assertThat(attempt.getAnswers()).hasSize(1);
        assertThat(attempt.getAnswers().get(0).getIsCorrect()).isTrue();
        assertThat(attempt.getAnswers().get(0).getIsFlagged()).isTrue();
    }

    @Test
    void submitDiagnostic_forbiddenIfWrongUser() {
        attempt.getUser().setId(999L); // khác userId=1
        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));

        SubmitDiagnosticRequest req = buildSubmitRequest("{\"selected\":\"A\"}");

        assertThatThrownBy(() -> service.submitDiagnostic(1L, req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void submitDiagnostic_alreadySubmitted_throwsBadRequest() {
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));

        SubmitDiagnosticRequest req = buildSubmitRequest("{\"selected\":\"A\"}");

        assertThatThrownBy(() -> service.submitDiagnostic(1L, req))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void submitDiagnostic_partDirective_PASS_noUserAbilitySaved() {
        // Score 100% → passScore=80 → 100 >= 80 → PASS directive → Tầng 2 bị bỏ qua
        setupSubmitMocksWithThreshold("{\"selected\":\"A\"}", 80.0f, null, null);

        SubmitDiagnosticRequest req = buildSubmitRequest("{\"selected\":\"A\"}");
        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(1);
        assertThat(resp.getTotalQuestions()).isEqualTo(1);
        assertThat(resp.getPartResults()).hasSize(1);
        assertThat(resp.getPartResults().get(0).getDirective()).isEqualTo("PASS");
        verify(userAbilityRepo).findByUserIdWithDetails(1L);
        verify(userAbilityRepo, never()).saveAll(anyList());
    }

    @Test
    void submitDiagnostic_partDirective_ABILITY_DIAGNOSTIC_savesUserAbilityWEAK() {
        setupSubmitMocksWithThreshold("{\"selected\":\"B\"}", 80.0f, 60.0f, null);

        AbilityEvaluationRule rule = new AbilityEvaluationRule();
        rule.setStableThreshold(75.0f);
        rule.setDevelopingThreshold(50.0f);
        rule.setMinEvidenceCount(1);
        rule.setAbility(ability1);
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(List.of(rule));
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);

        SubmitDiagnosticRequest req = buildSubmitRequest("{\"selected\":\"B\"}");
        service.submitDiagnostic(1L, req);

        verify(userAbilityRepo).saveAll(userAbilitiesCaptor.capture());
        List<UserAbility> saved = userAbilitiesCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(EAbilityStatus.WEAK);
    }

    @Test
    void submitDiagnostic_unansweredQuestionsInPart_dividesByTotalPartQuestions() {
        Question q2 = createQuestion(2L, part1, ability1, "{\"selected\":\"B\"}");
        Question q3 = createQuestion(3L, part1, ability1, "{\"selected\":\"C\"}");
        Question q4 = createQuestion(4L, part1, ability1, "{\"selected\":\"D\"}");

        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(question, q2, q3, q4));

        TargetPartThreshold threshold = buildThreshold(part1, 80.0f, 60.0f, 35.0f);
        when(targetPartThresholdRepo.findByTargetProfileId(anyLong())).thenReturn(List.of(threshold));

        AbilityEvaluationRule rule = new AbilityEvaluationRule();
        rule.setStableThreshold(75.0f);
        rule.setDevelopingThreshold(50.0f);
        rule.setMinEvidenceCount(1);
        rule.setAbility(ability1);
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(List.of(rule));
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        SubmitDiagnosticRequest req = buildSubmitRequestWithItems(List.of(
                buildAnswerItem(1L, "{\"selected\":\"A\"}"),
                buildAnswerItem(2L, "{\"selected\":\"B\"}")
        ));

        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(2);
        assertThat(resp.getTotalQuestions()).isEqualTo(4);
        assertThat(resp.getLearningPathId()).isNull();
        assertThat(resp.getPartResults()).hasSize(1);
        assertThat(resp.getPartResults().get(0).getScore()).isEqualTo(50.0f);
        assertThat(resp.getPartResults().get(0).getDirective()).isEqualTo("ABILITY_DIAGNOSTIC");

        verify(userAbilityRepo).saveAll(userAbilitiesCaptor.capture());
        UserAbility ua = userAbilitiesCaptor.getValue().get(0);
        assertThat(ua.getAccuracyRate()).isEqualTo(50.0f);
        assertThat(ua.getEvidenceCount()).isEqualTo(4);
        assertThat(ua.getStatus()).isEqualTo(EAbilityStatus.WEAK);
    }

    @Test
    void submitDiagnostic_unansweredEntirePart_createsFullPartResult() {
        Part part2 = new Part();
        part2.setId(2L);
        part2.setName("Part 2 - Question-Response");
        Question qPart2 = createQuestion(2L, part2, ability1, "{\"selected\":\"B\"}");

        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(question, qPart2));

        TargetPartThreshold t1 = buildThreshold(part1, 80.0f, 60.0f, 35.0f);
        TargetPartThreshold t2 = buildThreshold(part2, 80.0f, 60.0f, 35.0f);
        when(targetPartThresholdRepo.findByTargetProfileId(anyLong())).thenReturn(List.of(t1, t2));
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
        when(partRepo.getReferenceById(2L)).thenReturn(part2);
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        SubmitDiagnosticRequest req = buildSubmitRequest("{\"selected\":\"A\"}");
        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(1);
        assertThat(resp.getTotalQuestions()).isEqualTo(2);
        assertThat(resp.getPartResults()).hasSize(2);

        PartResultResponse part2Result = resp.getPartResults().stream()
                .filter(pr -> pr.getPartId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(part2Result.getScore()).isEqualTo(0.0f);
        assertThat(part2Result.getDirective()).isEqualTo("FULL_PART");
    }

    @Test
    void submitDiagnostic_duplicateQuestionIds_ignoresDuplicates() {
        Question q2 = createQuestion(2L, part1, ability1, "{\"selected\":\"B\"}");

        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(question, q2));

        TargetPartThreshold threshold = buildThreshold(part1, 80.0f, 60.0f, 35.0f);
        when(targetPartThresholdRepo.findByTargetProfileId(anyLong())).thenReturn(List.of(threshold));
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        SubmitDiagnosticRequest req = buildSubmitRequestWithItems(List.of(
                buildAnswerItem(1L, "{\"selected\":\"A\"}"),
                buildAnswerItem(1L, "{\"selected\":\"A\"}")
        ));

        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(1);
        assertThat(resp.getTotalQuestions()).isEqualTo(2);
        assertThat(resp.getPartResults().get(0).getScore()).isEqualTo(50.0f);
        assertThat(resp.getPartResults().get(0).getDirective()).isEqualTo("ABILITY_DIAGNOSTIC");
    }

    // =========================================================================
    // TEST getResultDetail & getLatestResultDetail (UC07)
    // =========================================================================

    @Test
    void getResultDetail_notSubmitted_throwsDiagnosticNotCompleted() {
        when(diagnosticAttemptRepo.findByIdWithContext(200L)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.getResultDetail(1L, 200L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.DIAGNOSTIC_NOT_COMPLETED);
    }

    @Test
    void getLatestResultDetail_success_returnsMappedReport() {
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.now());

        DiagnosticTestQuestion link = new DiagnosticTestQuestion();
        link.setOrderNo(1);
        link.setQuestion(question);

        DiagnosticAnswer ans = new DiagnosticAnswer();
        ans.setQuestion(question);
        ans.setIsCorrect(true);

        PartDiagnosticResult pr = new PartDiagnosticResult();
        pr.setPart(part1);
        pr.setScore(100.0f);
        pr.setDirective(EPartDirective.PASS);

        when(diagnosticAttemptRepo.findTopByUserIdAndStatusOrderByCompletedAtDesc(1L, EAttemptStatus.SUBMITTED))
                .thenReturn(Optional.of(attempt));
        when(diagnosticAttemptRepo.findByIdWithContext(200L)).thenReturn(Optional.of(attempt));
        when(partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(200L)).thenReturn(List.of(pr));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(200L)).thenReturn(List.of(ans));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(100L)).thenReturn(List.of(link));
        when(questionRepo.findAllWithAbilitiesByIdIn(List.of(1L))).thenReturn(List.of(question));
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(learningPathRepo.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        DiagnosticResultDetailResponse detail = service.getLatestResultDetail(1L);

        assertThat(detail.getAttemptId()).isEqualTo(200L);
        assertThat(detail.getPredictedScore()).isEqualTo(990);
        assertThat(detail.getTargetScore()).isEqualTo(650);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void setupSubmitMocksWithThreshold(String userAnswer, Float passScore, Float confirmingScore, Float fullPartThreshold) {
        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(question));

        TargetPartThreshold threshold = buildThreshold(part1, passScore, confirmingScore, fullPartThreshold);
        when(targetPartThresholdRepo.findByTargetProfileId(anyLong())).thenReturn(List.of(threshold));

        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
    }

    private TargetPartThreshold buildThreshold(Part part, Float passScore, Float confirmingScore, Float fullPartThreshold) {
        TargetPartThreshold threshold = new TargetPartThreshold();
        threshold.setPart(part);
        threshold.setPassScore(passScore);
        threshold.setConfirmingScore(confirmingScore);
        threshold.setFullPartThreshold(fullPartThreshold);
        return threshold;
    }

    private Question createQuestion(Long id, Part part, Ability ability, String correctAnswer) {
        QuestionAbility qa = new QuestionAbility();
        qa.setAbility(ability);
        Question q = new Question();
        q.setId(id);
        q.setPart(part);
        q.setCorrectAnswer(correctAnswer);
        q.setQuestionAbilities(List.of(qa));
        return q;
    }

    private SubmitDiagnosticRequest.AnswerItem buildAnswerItem(Long questionId, String userAnswer) {
        SubmitDiagnosticRequest.AnswerItem item = new SubmitDiagnosticRequest.AnswerItem();
        item.setQuestionId(questionId);
        item.setUserAnswer(userAnswer);
        return item;
    }

    private SubmitDiagnosticRequest buildSubmitRequestWithItems(List<SubmitDiagnosticRequest.AnswerItem> items) {
        SubmitDiagnosticRequest req = new SubmitDiagnosticRequest();
        req.setAttemptId(200L);
        req.setTargetProfileId(5L);
        req.setAnswers(items);
        req.setDurationSeconds(1800);
        return req;
    }

    private SubmitDiagnosticRequest buildSubmitRequest(String userAnswer) {
        return buildSubmitRequestWithItems(List.of(buildAnswerItem(1L, userAnswer)));
    }
}
