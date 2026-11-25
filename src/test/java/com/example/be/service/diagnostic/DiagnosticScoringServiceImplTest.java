package com.example.be.service.diagnostic;

import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.PartResultResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.repository.ability.UserAbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
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
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Kiểm tra luồng chấm điểm 2 tầng và phân loại UserAbility.
 */
@ExtendWith(MockitoExtension.class)
class DiagnosticScoringServiceImplTest {

    @Mock private DiagnosticTestRepository diagnosticTestRepo;
    @Mock private DiagnosticAttemptRepository diagnosticAttemptRepo;
    @Mock private QuestionRepository questionRepo;
    @Mock private TargetPartThresholdRepository targetPartThresholdRepo;
    @Mock private AbilityEvaluationRuleRepository abilityRuleRepo;
    @Mock private UserAbilityRepository userAbilityRepo;
    @Mock private UserRepository userRepo;
    @Mock private PartRepository partRepo;
    @Mock private AdaptivePathGeneratorService adaptivePathGeneratorService;

    @InjectMocks
    private DiagnosticScoringServiceImpl service;

    @Captor
    private ArgumentCaptor<List<UserAbility>> userAbilitiesCaptor;

    private User student;
    private DiagnosticTest diagnosticTest;
    private DiagnosticAttempt attempt;
    private Part part1;
    private Ability ability1;
    private Question question;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);

        part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1 - Photographs");

        ability1 = new Ability();
        ability1.setId(10L);

        diagnosticTest = new DiagnosticTest();
        diagnosticTest.setId(100L);
        diagnosticTest.setTestType(ETestType.DIAGNOSTIC);
        diagnosticTest.setDurationMinutes(60);
        diagnosticTest.setTestQuestions(Collections.emptyList());

        attempt = new DiagnosticAttempt();
        attempt.setId(200L);
        attempt.setUser(student);
        attempt.setDiagnosticTest(diagnosticTest);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);

        QuestionAbility qa = new QuestionAbility();
        qa.setAbility(ability1);

        question = new Question();
        question.setId(1L);
        question.setPart(part1);
        question.setCorrectAnswer("{\"selected\":\"A\"}");
        question.setQuestionAbilities(List.of(qa));
    }

    // =========================================================================
    // TEST startDiagnostic
    // =========================================================================

    @Test
    void startDiagnostic_success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);

        StartDiagnosticResponse resp = service.startDiagnostic(1L, req);

        assertThat(resp.getAttemptId()).isEqualTo(200L);
        assertThat(resp.getDurationMinutes()).isEqualTo(60);
    }

    @Test
    void startDiagnostic_wrongTestType_throwsBadRequest() {
        diagnosticTest.setTestType(ETestType.FULL_MOCK_TEST);
        when(userRepo.findById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(100L)).thenReturn(Optional.of(diagnosticTest));

        StartDiagnosticRequest req = new StartDiagnosticRequest();
        req.setDiagnosticTestId(100L);

        assertThatThrownBy(() -> service.startDiagnostic(1L, req))
                .isInstanceOf(BaseException.class);
    }

    // =========================================================================
    // TEST submitDiagnostic
    // =========================================================================

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
        // PASS không gọi Tầng 2 nên không lưu UserAbility
        verify(userAbilityRepo).findByUserIdWithDetails(1L);
        verify(userAbilityRepo, never()).saveAll(anyList());
    }

    @Test
    void submitDiagnostic_partDirective_ABILITY_DIAGNOSTIC_savesUserAbilityWEAK() {
        // userAnswer B ≠ correctAnswer A → score 0% → passScore=80, confirmingScore=60, fullPartThreshold=null → ABILITY_DIAGNOSTIC
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
        // 0% accuracy < 75% stableThreshold → WEAK
        assertThat(saved.get(0).getStatus()).isEqualTo(EAbilityStatus.WEAK);
    }

    @Test
    void submitDiagnostic_unansweredQuestionsInPart_dividesByTotalPartQuestions() {
        // Part 1 có 4 câu nhưng học viên chỉ gửi 2 câu đúng và bỏ trống 2 câu → phải tính 2/4 = 50%, KHÔNG phải 100%
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

        LearningPath lp = new LearningPath();
        lp.setId(999L);
        when(adaptivePathGeneratorService.generateAdaptivePath(any(), anyLong())).thenReturn(lp);

        SubmitDiagnosticRequest req = buildSubmitRequestWithItems(List.of(
                buildAnswerItem(1L, "{\"selected\":\"A\"}"),
                buildAnswerItem(2L, "{\"selected\":\"B\"}")
        ));

        DiagnosticResultResponse resp = service.submitDiagnostic(1L, req);

        assertThat(resp.getTotalCorrect()).isEqualTo(2);
        assertThat(resp.getTotalQuestions()).isEqualTo(4);
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
        // Đề có Part 1 (làm đúng) và Part 2 (bỏ trắng hoàn toàn) → Part 2 vẫn phải trả về 0% & FULL_PART
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

        LearningPath lp = new LearningPath();
        lp.setId(999L);
        when(adaptivePathGeneratorService.generateAdaptivePath(any(), anyLong())).thenReturn(lp);

        // Học viên chỉ trả lời câu thuộc Part 1, bỏ trắng Part 2
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
        // Part 1 có 2 câu; client gửi trùng câu 1 (đúng) 2 lần và bỏ câu 2 → chỉ tính 1/2 = 50%
        Question q2 = createQuestion(2L, part1, ability1, "{\"selected\":\"B\"}");

        when(diagnosticAttemptRepo.findById(200L)).thenReturn(Optional.of(attempt));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(question, q2));

        TargetPartThreshold threshold = buildThreshold(part1, 80.0f, 60.0f, 35.0f);
        when(targetPartThresholdRepo.findByTargetProfileId(anyLong())).thenReturn(List.of(threshold));
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(userAbilityRepo.findByUserIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
        when(diagnosticAttemptRepo.save(any())).thenReturn(attempt);

        LearningPath lp = new LearningPath();
        lp.setId(999L);
        when(adaptivePathGeneratorService.generateAdaptivePath(any(), anyLong())).thenReturn(lp);

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

        LearningPath lp = new LearningPath();
        lp.setId(999L);
        when(adaptivePathGeneratorService.generateAdaptivePath(any(), anyLong())).thenReturn(lp);
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
