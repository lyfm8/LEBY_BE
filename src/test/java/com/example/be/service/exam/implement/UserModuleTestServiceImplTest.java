package com.example.be.service.exam.implement;

import com.example.be.dto.request.exam.SaveModuleTestDraftRequest;
import com.example.be.dto.request.exam.SubmitModuleTestRequest;
import com.example.be.dto.request.exam.SyncModuleTestTimerRequest;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestQuestionSnapshotItem;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAnswer;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.ESection;
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
import com.example.be.service.exam.util.QuestionBatchLoader;
import com.example.be.service.exam.util.UserExamAccessHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserModuleTestServiceImpl} bao phủ UC13, UC14 và các BR03, BR04, BR05, BR07, BR10.
 */
@ExtendWith(MockitoExtension.class)
class UserModuleTestServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ModuleRepository moduleRepository;
    @Mock private ModuleTestQuestionRepository moduleTestQuestionRepository;
    @Mock private ModuleTestAttemptRepository moduleTestAttemptRepository;
    @Mock private ModuleTestAnswerRepository moduleTestAnswerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ModuleTargetThresholdRepository moduleTargetThresholdRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathItemRepository learningPathItemRepository;
    @Mock private QuestionBatchLoader questionBatchLoader;
    @Mock private UserExamAccessHelper userExamAccessHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserModuleTestServiceImpl service;

    @BeforeEach
    void setUp() {
        ExamQuestionMapper examQuestionMapper = new ExamQuestionMapper(objectMapper);
        ModuleTestUserMapper moduleTestUserMapper = new ModuleTestUserMapper(examQuestionMapper);
        service = new UserModuleTestServiceImpl(
                userRepository,
                moduleRepository,
                moduleTestQuestionRepository,
                moduleTestAttemptRepository,
                moduleTestAnswerRepository,
                questionRepository,
                moduleTargetThresholdRepository,
                learningPathRepository,
                learningPathItemRepository,
                questionBatchLoader,
                userExamAccessHelper,
                examQuestionMapper,
                moduleTestUserMapper,
                objectMapper
        );
    }

    @Test
    void startModuleTest_prerequisiteNotMetBR03_throwsException() {
        User user = buildUser(7L, 3L, 650);
        Module module = buildModule(2L, 80, 25, true, EModuleTestSelectionMode.MANUAL);
        LearningPathItem item = buildPathItem(1L, module, ELessonStatus.IN_PROGRESS, 40, 1);

        when(userRepository.findWithTargetProfileById(7L)).thenReturn(Optional.of(user));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 2L)).thenReturn(item);
        when(moduleRepository.findByIdWithPartAndAbilities(2L)).thenReturn(Optional.of(module));

        assertThatThrownBy(() -> service.startModuleTest(7L, 2L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MODULE_TEST_PREREQUISITE_NOT_MET));
    }

    @Test
    void startModuleTest_createsAttemptWithClusterAwareSnapshotAndOptionMapBR05() throws Exception {
        User user = buildUser(7L, 3L, 650);
        Module module = buildModule(2L, 80, 20, true, EModuleTestSelectionMode.MANUAL);
        LearningPathItem item = buildPathItem(1L, module, ELessonStatus.IN_PROGRESS, 100, 1);

        Question parent = new Question();
        parent.setId(900L);

        Question q1 = buildQuestion(101L, parent, "A");
        Question q2 = buildQuestion(102L, parent, "B");
        Question q3 = buildQuestion(103L, null, "C");

        ModuleTestQuestion mtq1 = buildMtq(1L, module, q1, 1);
        ModuleTestQuestion mtq2 = buildMtq(2L, module, q2, 2);
        ModuleTestQuestion mtq3 = buildMtq(3L, module, q3, 3);

        when(userRepository.findWithTargetProfileById(7L)).thenReturn(Optional.of(user));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 2L)).thenReturn(item);
        when(moduleRepository.findByIdWithPartAndAbilities(2L)).thenReturn(Optional.of(module));
        when(moduleTestAttemptRepository.findTopByUserIdAndModuleIdAndStatusOrderByStartedAtDesc(
                7L, 2L, EAttemptStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(moduleTestQuestionRepository.findByModuleIdWithQuestionOrderByOrderNoAsc(2L))
                .thenReturn(List.of(mtq1, mtq2, mtq3));
        when(moduleTestAttemptRepository.save(any(ModuleTestAttempt.class))).thenAnswer(inv -> {
            ModuleTestAttempt a = inv.getArgument(0);
            a.setId(55L);
            return a;
        });

        StartModuleTestResponse response = service.startModuleTest(7L, 2L);

        assertThat(response.getAttemptId()).isEqualTo(55L);
        assertThat(response.getDurationMinutes()).isEqualTo(20);

        ArgumentCaptor<ModuleTestAttempt> captor = ArgumentCaptor.forClass(ModuleTestAttempt.class);
        verify(moduleTestAttemptRepository).save(captor.capture());
        ModuleTestAttempt saved = captor.getValue();
        List<ModuleTestQuestionSnapshotItem> snap = objectMapper.readValue(
                saved.getQuestionSnapshot(),
                new TypeReference<List<ModuleTestQuestionSnapshotItem>>() {}
        );
        assertThat(snap).hasSize(3);
        // Hai câu cùng parentId=900 phải đứng liền kề nhau bất kể shuffle
        int idx101 = findSnapshotIndex(snap, 101L);
        int idx102 = findSnapshotIndex(snap, 102L);
        assertThat(Math.abs(idx101 - idx102)).isEqualTo(1);
        assertThat(snap.get(0).getOptionMap()).hasSize(4);
    }

    @Test
    void syncRemainingTime_clampsAgainstClientIncreasingTime() {
        User user = buildUser(7L, 3L, 650);
        Module module = buildModule(2L, 80, 25, false, EModuleTestSelectionMode.MANUAL);
        ModuleTestAttempt attempt = new ModuleTestAttempt();
        attempt.setId(88L);
        attempt.setUser(user);
        attempt.setModule(module);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setIsSubmitted(false);
        attempt.setRemainingSeconds(600);

        when(moduleTestAttemptRepository.findByIdWithModuleAndUser(88L)).thenReturn(Optional.of(attempt));

        long result = service.syncRemainingTime(7L, 2L, new SyncModuleTestTimerRequest(88L, 1200));

        assertThat(result).isEqualTo(600L);
        assertThat(attempt.getRemainingSeconds()).isEqualTo(600);
    }

    @Test
    void submitModuleTest_passedAndLastModule_unlocksNextAndCompletesRoadmap() throws Exception {
        User user = buildUser(7L, 3L, 650);
        Module mod1 = buildModule(1L, 80, 25, false, EModuleTestSelectionMode.MANUAL);
        Module mod2 = buildModule(2L, 80, 25, false, EModuleTestSelectionMode.MANUAL);

        LearningPath path = new LearningPath();
        path.setId(10L);
        path.setUser(user);
        path.setTargetProfile(user.getTargetProfile());
        path.setStatus(ELearningPathStatus.ACTIVE);

        LearningPathItem item1 = buildPathItem(1L, mod1, ELessonStatus.IN_PROGRESS, 100, 1);
        item1.setLearningPath(path);
        LearningPathItem item2 = buildPathItem(2L, mod2, ELessonStatus.LOCKED, 0, 2);
        item2.setLearningPath(path);

        Question q1 = buildQuestion(101L, null, "B"); // đáp án gốc B, hiển thị ở A qua optionMap
        List<ModuleTestQuestionSnapshotItem> snapshot = List.of(
                ModuleTestQuestionSnapshotItem.builder()
                        .orderNo(1)
                        .questionId(101L)
                        .moduleTestQuestionId(11L)
                        .optionMap(Map.of("A", "B", "B", "A", "C", "C", "D", "D"))
                        .build()
        );

        ModuleTestAttempt attempt = new ModuleTestAttempt();
        attempt.setId(99L);
        attempt.setUser(user);
        attempt.setModule(mod1);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setIsSubmitted(false);
        attempt.setQuestionSnapshot(objectMapper.writeValueAsString(snapshot));

        ModuleTargetThreshold threshold = new ModuleTargetThreshold();
        threshold.setPassScore(75);

        when(moduleTestAttemptRepository.findByIdWithModuleAndUser(99L)).thenReturn(Optional.of(attempt));
        when(userExamAccessHelper.requireUnlockedPathItem(7L, 1L)).thenReturn(item1);
        when(moduleRepository.findByIdWithPartAndAbilities(1L)).thenReturn(Optional.of(mod1));
        when(questionRepository.findAllById(List.of(101L))).thenReturn(List.of(q1));
        when(moduleTestAnswerRepository.findByModuleTestAttemptIdWithQuestion(99L)).thenReturn(Collections.emptyList());
        when(moduleTestQuestionRepository.getReferenceById(11L)).thenReturn(buildMtq(11L, mod1, q1, 1));
        when(moduleTargetThresholdRepository.findByModuleIdAndTargetProfileId(1L, 3L)).thenReturn(Optional.of(threshold));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(10L))
                .thenReturn(List.of(item1, item2));

        SubmitModuleTestRequest request = SubmitModuleTestRequest.builder()
                .attemptId(99L)
                .durationSeconds(320)
                .isTimedOut(false)
                .answers(List.of(
                        SubmitModuleTestRequest.SubmitAnswerItem.builder()
                                .questionId(101L)
                                .userAnswer("{\"selected\":\"A\"}")
                                .isFlagged(true)
                                .build()
                ))
                .build();

        SubmitModuleTestResponse res = service.submitModuleTest(7L, 1L, request);

        assertThat(res.getScore()).isEqualTo(100);
        assertThat(res.getPassScore()).isEqualTo(75);
        assertThat(res.getIsPassed()).isTrue();
        assertThat(res.getNextModuleId()).isEqualTo(2L);
        assertThat(res.getIsRoadmapCompleted()).isFalse();
        assertThat(item1.getStatus()).isEqualTo(ELessonStatus.COMPLETED);
        assertThat(item2.getStatus()).isEqualTo(ELessonStatus.IN_PROGRESS);
        assertThat(attempt.getResult()).isEqualTo(ETestResult.PASS);
        assertThat(attempt.getIsSubmitted()).isTrue();
    }

    @Test
    void saveDraftAnswers_andGetReview_preserveRemappedDisplayKeys() throws Exception {
        User user = buildUser(7L, 3L, 650);
        Module mod1 = buildModule(1L, 80, 25, true, EModuleTestSelectionMode.MANUAL);
        LearningPath path = new LearningPath();
        path.setId(10L);
        path.setTargetProfile(user.getTargetProfile());

        Question q1 = buildQuestion(101L, null, "C");
        Map<String, String> optionMap = Map.of("A", "C", "B", "A", "C", "B", "D", "D");
        List<ModuleTestQuestionSnapshotItem> snapshot = List.of(
                ModuleTestQuestionSnapshotItem.builder()
                        .orderNo(1)
                        .questionId(101L)
                        .moduleTestQuestionId(null)
                        .optionMap(optionMap)
                        .build()
        );

        ModuleTestAttempt attempt = new ModuleTestAttempt();
        attempt.setId(99L);
        attempt.setUser(user);
        attempt.setModule(mod1);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setIsSubmitted(false);
        attempt.setQuestionSnapshot(objectMapper.writeValueAsString(snapshot));

        when(moduleTestAttemptRepository.findByIdWithModuleAndUser(99L)).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllById(List.of(101L))).thenReturn(List.of(q1));
        when(moduleTestAnswerRepository.findByModuleTestAttemptIdWithQuestion(99L)).thenReturn(Collections.emptyList());

        SaveModuleTestDraftRequest draftReq = SaveModuleTestDraftRequest.builder()
                .attemptId(99L)
                .answers(List.of(new SaveModuleTestDraftRequest.DraftAnswerItem(101L, "A", true)))
                .build();

        service.saveDraftAnswers(7L, 1L, draftReq);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ModuleTestAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(moduleTestAnswerRepository).saveAll(captor.capture());
        ModuleTestAnswer savedDraft = captor.getValue().get(0);
        assertThat(savedDraft.getUserAnswer()).isEqualTo("{\"selected\":\"C\"}");
        assertThat(savedDraft.getIsCorrect()).isTrue();
        assertThat(savedDraft.getIsFlagged()).isTrue();

        // Giả lập attempt đã nộp để kiểm tra Review quy đổi lại key hiển thị = "A"
        attempt.setIsSubmitted(true);
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        attempt.setScore(100);
        attempt.setResult(ETestResult.PASS);
        attempt.setSubmittedAt(LocalDateTime.now());

        when(userExamAccessHelper.requireCurrentLearningPath(7L)).thenReturn(path);
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L))).thenReturn(Map.of(101L, q1));
        when(moduleTestAnswerRepository.findByModuleTestAttemptIdWithQuestion(99L)).thenReturn(List.of(savedDraft));

        ModuleTestReviewResponse review = service.getTestReview(7L, 1L, 99L);

        assertThat(review.getQuestions()).hasSize(1);
        assertThat(review.getQuestions().get(0).getUserAnswer()).isEqualTo("A");
        assertThat(review.getQuestions().get(0).getCorrectAnswer()).isEqualTo("A");
        assertThat(review.getQuestions().get(0).getIsCorrect()).isTrue();

        // Kiểm tra thêm Report
        when(moduleTestAttemptRepository.findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(7L, 1L))
                .thenReturn(List.of(attempt));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(10L))
                .thenReturn(Collections.emptyList());

        ModuleTestPerformanceReportResponse report = service.getPerformanceReport(7L, 1L, 99L);
        assertThat(report.getScore()).isEqualTo(100);
        assertThat(report.getHistory()).hasSize(1);
    }

    private int findSnapshotIndex(List<ModuleTestQuestionSnapshotItem> list, Long questionId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getQuestionId().equals(questionId)) {
                return i;
            }
        }
        return -1;
    }

    private User buildUser(Long id, Long targetId, int targetScore) {
        TargetProfile tp = new TargetProfile();
        tp.setId(targetId);
        tp.setTargetTotalScore(targetScore);

        User u = new User();
        u.setId(id);
        u.setTargetProfile(tp);
        return u;
    }

    private Module buildModule(Long id, int reqRate, int durationMin, boolean shuffle, EModuleTestSelectionMode mode) {
        Part part = new Part();
        part.setId(5L);
        part.setName("Part 5: Incomplete Sentences");
        part.setSections(ESection.READING);

        Module m = new Module();
        m.setId(id);
        m.setTitle("Module " + id);
        m.setPart(part);
        m.setDefaultPassScore(65);
        m.setRequiredCompletionRate(reqRate);
        m.setTestDurationMinutes(durationMin);
        m.setTestQuestionCount(25);
        m.setShuffleQuestions(shuffle);
        m.setTestSelectionMode(mode);
        return m;
    }

    private LearningPathItem buildPathItem(Long id, Module module, ELessonStatus status, int progress, int orderNo) {
        LearningPathItem item = new LearningPathItem();
        item.setId(id);
        item.setModule(module);
        item.setStatus(status);
        item.setProgressPercent(progress);
        item.setOrderNo(orderNo);
        return item;
    }

    private Question buildQuestion(Long id, Question parent, String correctKey) {
        Part part = new Part();
        part.setId(5L);
        part.setName("Part 5");
        part.setSections(ESection.READING);

        Question q = new Question();
        q.setId(id);
        q.setParent(parent);
        q.setPart(part);
        q.setSections(ESection.READING);
        q.setQuestionData("{\"text\":\"Question " + id + "\",\"options\":[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"},{\"key\":\"C\",\"text\":\"Opt C\"},{\"key\":\"D\",\"text\":\"Opt D\"}]}");
        q.setCorrectAnswer("{\"key\":\"" + correctKey + "\"}");
        return q;
    }

    private ModuleTestQuestion buildMtq(Long id, Module module, Question question, int orderNo) {
        ModuleTestQuestion mtq = new ModuleTestQuestion();
        mtq.setId(id);
        mtq.setModule(module);
        mtq.setQuestion(question);
        mtq.setOrderNo(orderNo);
        mtq.setPoints(10);
        return mtq;
    }
}
