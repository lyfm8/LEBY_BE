package com.example.be.service.mockexam;

import com.example.be.dto.request.mockexam.SaveMockExamDraftRequest;
import com.example.be.dto.request.mockexam.StartMockExamRequest;
import com.example.be.dto.request.mockexam.SubmitMockExamRequest;
import com.example.be.dto.request.mockexam.SyncMockExamTimerRequest;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.dto.response.mockexam.MockExamListResponse;
import com.example.be.dto.response.mockexam.MockExamReportResponse;
import com.example.be.dto.response.mockexam.MockExamReviewResponse;
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
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.diagnostic.DiagnosticReviewMapper;
import com.example.be.mapper.diagnostic.DiagnosticRoomMapper;
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
import com.example.be.service.mockexam.implement.UserMockExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserMockExamServiceImpl} (UC17 & UC18).
 */
@ExtendWith(MockitoExtension.class)
class UserMockExamServiceImplTest {

    @Mock private DiagnosticTestRepository diagnosticTestRepo;
    @Mock private DiagnosticAttemptRepository diagnosticAttemptRepo;
    @Mock private DiagnosticTestQuestionRepository diagnosticTestQuestionRepo;
    @Mock private DiagnosticAnswerRepository diagnosticAnswerRepo;
    @Mock private PartDiagnosticResultRepository partDiagnosticResultRepo;
    @Mock private QuestionRepository questionRepo;
    @Mock private UserRepository userRepo;
    @Mock private TargetProfileRepository targetProfileRepo;
    @Mock private PartRepository partRepo;
    @Mock private TargetPartThresholdRepository targetPartThresholdRepo;
    @Mock private AbilityEvaluationRuleRepository abilityRuleRepo;
    @Mock private ToeicScoreConversionRepository toeicScoreConversionRepo;
    @Mock private LearningPathRepository learningPathRepo;
    @Mock private LearningPathItemRepository learningPathItemRepo;
    @Mock private QuestionBatchLoader questionBatchLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Spy private ExamQuestionMapper examQuestionMapper = new ExamQuestionMapper(objectMapper);
    @Spy private DiagnosticRoomMapper diagnosticRoomMapper = new DiagnosticRoomMapper(examQuestionMapper);
    @Spy private DiagnosticReviewMapper diagnosticReviewMapper = new DiagnosticReviewMapper(diagnosticRoomMapper, examQuestionMapper);
    @Spy private MockExamRoomMapper mockExamRoomMapper = new MockExamRoomMapper(diagnosticRoomMapper, diagnosticReviewMapper, examQuestionMapper);
    @Spy private MockExamReportMapper mockExamReportMapper = new MockExamReportMapper(examQuestionMapper);

    @InjectMocks
    private UserMockExamServiceImpl service;

    private User student;
    private TargetProfile target750;
    private DiagnosticTest practiceTest;
    private DiagnosticTest outputTest;
    private Part part1;
    private Part part5;
    private Question qListening;
    private Question qReading;

    @BeforeEach
    void setUp() {
        target750 = new TargetProfile();
        target750.setId(4L);
        target750.setTargetTotalScore(750);
        target750.setStatus(true);

        student = new User();
        student.setId(1L);
        student.setTargetProfile(target750);

        practiceTest = new DiagnosticTest();
        practiceTest.setId(10L);
        practiceTest.setTitle("Đề luyện tập ETS #1");
        practiceTest.setTestType(ETestType.FULL_MOCK_TEST);
        practiceTest.setExamTag(EExamTag.PRACTICE);
        practiceTest.setExamStatus(EExamStatus.ACTIVE);
        practiceTest.setDurationMinutes(120);

        outputTest = new DiagnosticTest();
        outputTest.setId(2L);
        outputTest.setTitle("Đề thi đầu ra ETS 2026");
        outputTest.setTestType(ETestType.FULL_MOCK_TEST);
        outputTest.setExamTag(EExamTag.OUTPUT);
        outputTest.setExamStatus(EExamStatus.ACTIVE);
        outputTest.setDurationMinutes(120);

        part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1 - Photographs");

        part5 = new Part();
        part5.setId(5L);
        part5.setName("Part 5 - Incomplete Sentences");

        qListening = new Question();
        qListening.setId(101L);
        qListening.setPart(part1);
        qListening.setCorrectAnswer("{\"key\":\"A\"}");

        qReading = new Question();
        qReading.setId(102L);
        qReading.setPart(part5);
        qReading.setCorrectAnswer("{\"key\":\"B\"}");
    }

    @Test
    void listAvailableMockExams_locksOutputWhenRoadmapNotCompleted() {
        LearningPath activePath = new LearningPath();
        activePath.setId(15L);
        activePath.setStatus(ELearningPathStatus.ACTIVE);
        LearningPathItem pendingItem = new LearningPathItem();
        pendingItem.setStatus(ELessonStatus.IN_PROGRESS);

        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(learningPathRepo.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepo.findByLearningPathIdWithDetailsOrderByOrderNoAsc(15L))
                .thenReturn(List.of(pendingItem));
        when(diagnosticTestRepo.findActiveByTestTypeOrdered(ETestType.FULL_MOCK_TEST, EExamStatus.ACTIVE))
                .thenReturn(List.of(practiceTest, outputTest));
        when(diagnosticAttemptRepo.findAllByUserIdAndTestTypeWithContextOrderByRecent(1L, ETestType.FULL_MOCK_TEST))
                .thenReturn(Collections.emptyList());
        when(toeicScoreConversionRepo.findAll()).thenReturn(Collections.emptyList());

        MockExamListResponse resp = service.listAvailableMockExams(1L);

        assertThat(resp.getIsOutputExamUnlocked()).isFalse();
        assertThat(resp.getExams()).hasSize(2);
        assertThat(resp.getExams().get(0).getExamTag()).isEqualTo(EExamTag.PRACTICE);
        assertThat(resp.getExams().get(0).getIsLocked()).isFalse();
        assertThat(resp.getExams().get(1).getExamTag()).isEqualTo(EExamTag.OUTPUT);
        assertThat(resp.getExams().get(1).getIsLocked()).isTrue();
    }

    @Test
    void startMockExam_outputLockedWhenRoadmapNotDone_throwsOutputLocked() {
        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(2L)).thenReturn(Optional.of(outputTest));
        when(learningPathRepo.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(Collections.emptyList());

        StartMockExamRequest req = new StartMockExamRequest();
        req.setDiagnosticTestId(2L);

        assertThatThrownBy(() -> service.startMockExam(1L, req))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.MOCK_EXAM_OUTPUT_LOCKED);
    }

    @Test
    void startMockExam_outputUnlockedWhenRoadmapCompleted_createsAttempt120Minutes() {
        LearningPath completedPath = new LearningPath();
        completedPath.setId(20L);
        completedPath.setStatus(ELearningPathStatus.COMPLETED);

        when(userRepo.findWithTargetProfileById(1L)).thenReturn(Optional.of(student));
        when(diagnosticTestRepo.findById(2L)).thenReturn(Optional.of(outputTest));
        when(learningPathRepo.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(List.of(completedPath));
        when(diagnosticAttemptRepo.findTopByUserIdAndDiagnosticTestTestTypeAndStatusOrderByStartedAtDesc(
                1L, ETestType.FULL_MOCK_TEST, EAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(diagnosticAttemptRepo.save(any())).thenAnswer(inv -> {
            DiagnosticAttempt saved = inv.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        StartMockExamRequest req = new StartMockExamRequest();
        req.setDiagnosticTestId(2L);

        StartDiagnosticResponse resp = service.startMockExam(1L, req);

        assertThat(resp.getAttemptId()).isEqualTo(500L);
        assertThat(resp.getDiagnosticTestId()).isEqualTo(2L);
        assertThat(resp.getDurationMinutes()).isEqualTo(120);
    }

    @Test
    void saveDraftAnswers_upsertsExistingDraftAndValidatesOwnership() {
        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(300L);
        attempt.setUser(student);
        attempt.setDiagnosticTest(practiceTest);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);

        DiagnosticTestQuestion link = new DiagnosticTestQuestion();
        link.setOrderNo(1);
        link.setQuestion(qListening);

        DiagnosticAnswer existingDraft = new DiagnosticAnswer();
        existingDraft.setId(88L);
        existingDraft.setDiagnosticAttempt(attempt);
        existingDraft.setQuestion(qListening);
        existingDraft.setUserAnswer("{\"selected\":\"B\"}");
        existingDraft.setIsCorrect(false);

        when(diagnosticAttemptRepo.findByIdWithContext(300L)).thenReturn(Optional.of(attempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(10L)).thenReturn(List.of(link));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(300L)).thenReturn(List.of(existingDraft));

        SaveMockExamDraftRequest req = new SaveMockExamDraftRequest();
        req.setAttemptId(300L);
        SaveMockExamDraftRequest.DraftAnswerItem item = new SaveMockExamDraftRequest.DraftAnswerItem();
        item.setQuestionId(101L);
        item.setSelectedOption("A");
        item.setIsFlagged(true);
        req.setAnswers(List.of(item));

        service.saveDraftAnswers(1L, req);

        ArgumentCaptor<List<DiagnosticAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(diagnosticAnswerRepo).saveAll(captor.capture());
        DiagnosticAnswer saved = captor.getValue().get(0);
        assertThat(saved.getId()).isEqualTo(88L);
        assertThat(saved.getUserAnswer()).isEqualTo("{\"selected\":\"A\"}");
        assertThat(saved.getIsCorrect()).isTrue();
        assertThat(saved.getIsFlagged()).isTrue();
    }

    @Test
    void syncRemainingTime_clampsAndRejectsOtherUsers() {
        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(300L);
        attempt.setUser(student);
        attempt.setDiagnosticTest(practiceTest);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setRemainingSeconds(4000);

        when(diagnosticAttemptRepo.findByIdWithContext(300L)).thenReturn(Optional.of(attempt));
        when(diagnosticAttemptRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncMockExamTimerRequest req = new SyncMockExamTimerRequest();
        req.setAttemptId(300L);
        req.setRemainingSeconds(5000); // cố gửi lớn hơn 4000 -> bị clamp về 4000
        assertThat(service.syncRemainingTime(1L, req)).isEqualTo(4000L);

        req.setRemainingSeconds(3850);
        assertThat(service.syncRemainingTime(1L, req)).isEqualTo(3850L);

        assertThatThrownBy(() -> service.syncRemainingTime(999L, req))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void submitMockExam_scoresEtsAndSavesPartResults() {
        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(300L);
        attempt.setUser(student);
        attempt.setDiagnosticTest(outputTest);
        attempt.setTargetProfile(target750);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(qListening);
        DiagnosticTestQuestion link2 = new DiagnosticTestQuestion();
        link2.setOrderNo(2);
        link2.setQuestion(qReading);

        ToeicScoreConversion conv100 = new ToeicScoreConversion();
        conv100.setCorrectCount(100);
        conv100.setListeningScore(495);
        conv100.setReadingScore(495);

        when(diagnosticAttemptRepo.findByIdWithContext(300L)).thenReturn(Optional.of(attempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(2L))
                .thenReturn(List.of(link1, link2));
        when(questionRepo.findAllWithAbilitiesByIdIn(anyList())).thenReturn(List.of(qListening, qReading));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(300L)).thenReturn(Collections.emptyList());
        when(targetPartThresholdRepo.findByTargetProfileId(4L)).thenReturn(Collections.emptyList());
        when(partRepo.getReferenceById(1L)).thenReturn(part1);
        when(partRepo.getReferenceById(5L)).thenReturn(part5);
        when(toeicScoreConversionRepo.findAll()).thenReturn(List.of(conv100));

        SubmitMockExamRequest req = new SubmitMockExamRequest();
        req.setAttemptId(300L);
        req.setDurationSeconds(5400);
        SubmitMockExamRequest.AnswerItem ans1 = new SubmitMockExamRequest.AnswerItem();
        ans1.setQuestionId(101L);
        ans1.setUserAnswer("{\"selected\":\"A\"}");
        SubmitMockExamRequest.AnswerItem ans2 = new SubmitMockExamRequest.AnswerItem();
        ans2.setQuestionId(102L);
        ans2.setUserAnswer("{\"selected\":\"B\"}");
        req.setAnswers(List.of(ans1, ans2));

        MockExamSubmitResponse resp = service.submitMockExam(1L, req);

        assertThat(resp.getTotalToeicScore()).isEqualTo(990);
        assertThat(resp.getTotalCorrect()).isEqualTo(2);
        assertThat(resp.getIsAimAchieved()).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(EAttemptStatus.SUBMITTED);
        verify(partDiagnosticResultRepo).saveAll(anyList());
    }

    @Test
    void getReport_outputExam_loadsBeforeDiagnosticPartResultsFromLearningPath() {
        DiagnosticAttempt initialDiag = new DiagnosticAttempt();
        initialDiag.setId(55L);

        LearningPath completedPath = new LearningPath();
        completedPath.setId(20L);
        completedPath.setStatus(ELearningPathStatus.COMPLETED);
        completedPath.setTargetProfile(target750);
        completedPath.setDiagnosticAttempt(initialDiag);

        DiagnosticAttempt mockAttempt = new DiagnosticAttempt();
        mockAttempt.setId(300L);
        mockAttempt.setUser(student);
        mockAttempt.setDiagnosticTest(outputTest);
        mockAttempt.setTargetProfile(target750);
        mockAttempt.setStatus(EAttemptStatus.SUBMITTED);
        mockAttempt.setCompletedAt(LocalDateTime.now());

        PartDiagnosticResult beforeP1 = new PartDiagnosticResult();
        beforeP1.setPart(part1);
        beforeP1.setScore(50.0f);
        beforeP1.setDirective(EPartDirective.CONFIRM);

        PartDiagnosticResult afterP1 = new PartDiagnosticResult();
        afterP1.setPart(part1);
        afterP1.setScore(100.0f);
        afterP1.setDirective(EPartDirective.PASS);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(qListening);

        DiagnosticAnswer mockAns1 = new DiagnosticAnswer();
        mockAns1.setQuestion(qListening);
        mockAns1.setIsCorrect(true);

        when(diagnosticAttemptRepo.findByIdWithContext(300L)).thenReturn(Optional.of(mockAttempt));
        when(partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(300L)).thenReturn(List.of(afterP1));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(300L)).thenReturn(List.of(mockAns1));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(2L)).thenReturn(List.of(link1));
        when(questionRepo.findAllWithAbilitiesByIdIn(List.of(101L))).thenReturn(List.of(qListening));
        when(toeicScoreConversionRepo.findAll()).thenReturn(Collections.emptyList());
        when(learningPathRepo.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(List.of(completedPath));
        when(partDiagnosticResultRepo.findByDiagnosticAttemptIdWithPart(55L)).thenReturn(List.of(beforeP1));
        when(abilityRuleRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(targetPartThresholdRepo.findByTargetProfileId(4L)).thenReturn(Collections.emptyList());

        MockExamReportResponse report = service.getReport(1L, 300L);

        assertThat(report.getBeforeRadar()).hasSize(7);
        assertThat(report.getAfterRadar()).hasSize(7);
        assertThat(report.getBeforeRadar().get(0).getScore()).isEqualTo(50);
        assertThat(report.getAfterRadar().get(0).getScore()).isEqualTo(100);
        assertThat(report.getPartResults().get(0).getScoreDelta()).isEqualTo(50);
    }

    @Test
    void getReviewData_outputExamBelowAim_throwsMockExamReviewLocked() {
        DiagnosticAttempt mockAttempt = new DiagnosticAttempt();
        mockAttempt.setId(301L);
        mockAttempt.setUser(student);
        mockAttempt.setDiagnosticTest(outputTest);
        mockAttempt.setTargetProfile(target750);
        mockAttempt.setStatus(EAttemptStatus.SUBMITTED);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(qListening);
        DiagnosticTestQuestion link2 = new DiagnosticTestQuestion();
        link2.setOrderNo(2);
        link2.setQuestion(qReading);

        DiagnosticAnswer wrongListening = new DiagnosticAnswer();
        wrongListening.setQuestion(qListening);
        wrongListening.setIsCorrect(false);
        DiagnosticAnswer wrongReading = new DiagnosticAnswer();
        wrongReading.setQuestion(qReading);
        wrongReading.setIsCorrect(false);

        when(diagnosticAttemptRepo.findByIdWithContext(301L)).thenReturn(Optional.of(mockAttempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(2L))
                .thenReturn(List.of(link1, link2));
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L, 102L)))
                .thenReturn(Map.of(101L, qListening, 102L, qReading));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(301L))
                .thenReturn(List.of(wrongListening, wrongReading));
        when(toeicScoreConversionRepo.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getReviewData(1L, 301L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MOCK_EXAM_REVIEW_LOCKED));
    }

    @Test
    void getReviewData_outputExamMeetsAim_returnsReviewResponse() {
        DiagnosticAttempt mockAttempt = new DiagnosticAttempt();
        mockAttempt.setId(302L);
        mockAttempt.setUser(student);
        mockAttempt.setDiagnosticTest(outputTest);
        mockAttempt.setTargetProfile(target750);
        mockAttempt.setStatus(EAttemptStatus.SUBMITTED);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(qListening);
        DiagnosticTestQuestion link2 = new DiagnosticTestQuestion();
        link2.setOrderNo(2);
        link2.setQuestion(qReading);

        DiagnosticAnswer correctListening = new DiagnosticAnswer();
        correctListening.setQuestion(qListening);
        correctListening.setIsCorrect(true);
        DiagnosticAnswer correctReading = new DiagnosticAnswer();
        correctReading.setQuestion(qReading);
        correctReading.setIsCorrect(true);

        when(diagnosticAttemptRepo.findByIdWithContext(302L)).thenReturn(Optional.of(mockAttempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(2L))
                .thenReturn(List.of(link1, link2));
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L, 102L)))
                .thenReturn(Map.of(101L, qListening, 102L, qReading));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(302L))
                .thenReturn(List.of(correctListening, correctReading));
        when(toeicScoreConversionRepo.findAll()).thenReturn(Collections.emptyList());

        MockExamReviewResponse review = service.getReviewData(1L, 302L);

        assertThat(review.getAttemptId()).isEqualTo(302L);
        assertThat(review.getTotalQuestions()).isEqualTo(2);
    }

    @Test
    void getReviewData_practiceExamBelowAim_stillAllowsReview() {
        DiagnosticAttempt practiceAttempt = new DiagnosticAttempt();
        practiceAttempt.setId(303L);
        practiceAttempt.setUser(student);
        practiceAttempt.setDiagnosticTest(practiceTest);
        practiceAttempt.setTargetProfile(target750);
        practiceAttempt.setStatus(EAttemptStatus.SUBMITTED);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(qListening);

        DiagnosticAnswer wrongListening = new DiagnosticAnswer();
        wrongListening.setQuestion(qListening);
        wrongListening.setIsCorrect(false);

        when(diagnosticAttemptRepo.findByIdWithContext(303L)).thenReturn(Optional.of(practiceAttempt));
        when(diagnosticTestQuestionRepo.findAllByDiagnosticTestIdOrderByOrderNoAsc(10L))
                .thenReturn(List.of(link1));
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L)))
                .thenReturn(Map.of(101L, qListening));
        when(diagnosticAnswerRepo.findByDiagnosticAttemptIdWithQuestion(303L))
                .thenReturn(List.of(wrongListening));

        MockExamReviewResponse review = service.getReviewData(1L, 303L);

        assertThat(review.getAttemptId()).isEqualTo(303L);
        assertThat(review.getExamTag()).isEqualTo(EExamTag.PRACTICE);
    }
}
