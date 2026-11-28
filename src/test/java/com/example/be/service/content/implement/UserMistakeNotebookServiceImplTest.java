package com.example.be.service.content.implement;

import com.example.be.dto.request.content.SubmitMistakeRetryRequest;
import com.example.be.dto.request.content.SubmitMistakeRetryRequest.MistakeRetryAnswerItem;
import com.example.be.dto.response.content.MistakeNotebookResponse;
import com.example.be.dto.response.content.MistakeRetrySetResponse;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.EMistakeSourceType;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserMistakeNotebookMapper;
import com.example.be.mapper.mockexam.MockExamReportMapper;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.repository.content.UserMistakeQueryRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.setting.ToeicScoreConversionRepository;
import com.example.be.service.exam.util.QuestionBatchLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMistakeNotebookServiceImplTest {

    private static final Long USER_ID = 42L;

    @Mock
    private UserMistakeQueryRepository userMistakeQueryRepository;

    @Mock
    private QuestionAbilityRepository questionAbilityRepository;

    @Mock
    private ToeicScoreConversionRepository toeicScoreConversionRepository;

    @Mock
    private QuestionBatchLoader questionBatchLoader;

    private UserMistakeNotebookServiceImpl service;

    @BeforeEach
    void setUp() {
        ExamQuestionMapper examQuestionMapper = new ExamQuestionMapper(new ObjectMapper());
        MockExamReportMapper mockExamReportMapper = new MockExamReportMapper(examQuestionMapper);
        UserMistakeNotebookMapper mapper = new UserMistakeNotebookMapper(examQuestionMapper);
        service = new UserMistakeNotebookServiceImpl(
                userMistakeQueryRepository,
                questionAbilityRepository,
                toeicScoreConversionRepository,
                questionBatchLoader,
                mockExamReportMapper,
                mapper
        );
    }

    @Test
    void getMistakeNotebook_whenUserHasNoWrongAnswers_returnsEmptyWithoutBatchQueries() {
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());

        MistakeNotebookResponse response = service.getMistakeNotebook(USER_ID, null, null, null, null, 0, 10);

        assertThat(response.getTotalWrongCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        verify(questionBatchLoader, never()).loadDetailedQuestionsMap(anyCollection());
    }

    @Test
    void getMistakeNotebook_deduplicatesAcrossMockExamAndModuleTestAndKeepsLatestWrongAnswer() {
        LocalDateTime olderTime = LocalDateTime.of(2026, 11, 20, 9, 0);
        LocalDateTime newerTime = LocalDateTime.of(2026, 11, 27, 15, 30);

        // Câu 101 sai cả ở Đề thi thử PRACTICE (cũ hơn) lẫn ModuleTest (mới hơn); Câu 202 sai ở Đề thi thử PRACTICE
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(List.of(
                        new Object[]{101L, "{\"selected\":\"B\"}", "ETS Full Test 1", 10L, olderTime, EExamTag.PRACTICE, 650},
                        new Object[]{202L, "{\"selected\":\"C\"}", "ETS Full Test 1", 10L, olderTime, EExamTag.PRACTICE, 650}
                ));
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{101L, "{\"selected\":\"D\"}", "Từ loại Part 5", 20L, newerTime}
                ));

        when(userMistakeQueryRepository.findQuestionMetaByIdIn(anyCollection()))
                .thenReturn(List.of(
                        new Object[]{101L, 5L, "Part 5", "Câu ngữ pháp từ loại"},
                        new Object[]{202L, 7L, "Part 7", "Câu đọc hiểu email"}
                ));

        Question q101 = buildQuestion(101L, 5L, "Part 5", ESection.READING, "A",
                "{\"text\":\"The manager _____ the report yesterday.\",\"options\":[{\"key\":\"A\",\"text\":\"reviewed\"},{\"key\":\"B\",\"text\":\"review\"},{\"key\":\"C\",\"text\":\"reviewing\"},{\"key\":\"D\",\"text\":\"reviews\"}]}",
                "{\"clue\":\"Có yesterday -> quá khứ đơn\",\"analysis\":\"Chọn động từ chia quá khứ đơn.\"}");
        Ability ability101 = buildAbility(15L, "Thì động từ");
        QuestionAbility qa101 = new QuestionAbility();
        qa101.setQuestion(q101);
        qa101.setAbility(ability101);

        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(anyCollection()))
                .thenReturn(List.of(qa101));
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L)))
                .thenReturn(Map.of(101L, q101));

        MistakeNotebookResponse response = service.getMistakeNotebook(USER_ID, 5L, null, null, null, 0, 10);

        assertThat(response.getTotalWrongCount()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuestionId()).isEqualTo(101L);
        assertThat(response.getItems().get(0).getWrongCount()).isEqualTo(2);
        assertThat(response.getItems().get(0).getLastUserAnswer()).isEqualTo("D");
        assertThat(response.getItems().get(0).getCorrectAnswer()).isEqualTo("A");
        assertThat(response.getItems().get(0).getSourceType()).isEqualTo(EMistakeSourceType.MODULE_TEST);
        assertThat(response.getItems().get(0).getExplanation()).isNotNull();
        assertThat(response.getItems().get(0).getExplanation().getClue()).contains("yesterday");
        assertThat(response.getFilterOptions().getParts()).hasSize(2);
    }

    @Test
    void getMistakeNotebook_excludesLockedOutputExamBelowAimFromReviewAndRetry() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 27, 10, 0);

        // Attempt 50: Đề đầu ra OUTPUT mục tiêu AIM 750 nhưng điểm quy đổi ~300 -> không trả ra Sổ tay
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{301L, "{\"selected\":\"B\"}", "Đề đầu ra ETS #1", 50L, time, EExamTag.OUTPUT, 750}
                ));
        when(userMistakeQueryRepository.countOutputAttemptPartStatsByAttemptIds(anyCollection()))
                .thenReturn(List.of(
                        new Object[]{50L, 1L, ESection.LISTENING, "Part 1", 100L, 30L},
                        new Object[]{50L, 5L, ESection.READING, "Part 5", 100L, 30L}
                ));
        when(toeicScoreConversionRepository.findAll()).thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());

        MistakeNotebookResponse response = service.getMistakeNotebook(USER_ID, null, null, null, null, 0, 10);

        assertThat(response.getTotalWrongCount()).isZero();
        assertThat(response.getItems()).isEmpty();
        verify(questionBatchLoader, never()).loadDetailedQuestionsMap(anyCollection());

        // Chặn cả API chấm lại (chống answer-harvesting cho câu OUTPUT đang khóa)
        SubmitMistakeRetryRequest retryReq = SubmitMistakeRetryRequest.builder()
                .answers(List.of(MistakeRetryAnswerItem.builder().questionId(301L).selectedAnswer("A").build()))
                .build();
        assertThatThrownBy(() -> service.submitRetryAnswers(USER_ID, retryReq))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getMistakeNotebook_exposesOutputExamMistakesOnceAimTargetIsReached() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 28, 9, 0);

        // Attempt 60: Đề đầu ra OUTPUT mục tiêu AIM 650, đúng 80/100 Nghe + 80/100 Đọc (~790 điểm >= 650) -> MỞ KHÓA
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{401L, "{\"selected\":\"B\"}", "Đề đầu ra ETS #2", 60L, time, EExamTag.OUTPUT, 650}
                ));
        // Kiểm chứng dùng p.sections (LISTENING/READING) thay vì hardcode partId <= 4 (thử với partId = 10 thuộc LISTENING)
        when(userMistakeQueryRepository.countOutputAttemptPartStatsByAttemptIds(anyCollection()))
                .thenReturn(List.of(
                        new Object[]{60L, 10L, ESection.LISTENING, "Part 2", 100L, 80L},
                        new Object[]{60L, 17L, ESection.READING, "Part 7", 100L, 80L}
                ));
        when(toeicScoreConversionRepository.findAll()).thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findQuestionMetaByIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{401L, 7L, "Part 7", "Câu đọc hiểu"}));
        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(anyCollection()))
                .thenReturn(Collections.emptyList());

        Question q401 = buildQuestion(401L, 7L, "Part 7", ESection.READING, "A", "{\"text\":\"Reading Q\"}", "{\"clue\":\"Clue\"}");
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(401L)))
                .thenReturn(Map.of(401L, q401));

        MistakeNotebookResponse response = service.getMistakeNotebook(USER_ID, null, null, null, null, 0, 10);

        assertThat(response.getTotalWrongCount()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuestionId()).isEqualTo(401L);
    }

    @Test
    void getRetryQuestionSet_returnsFilteredQuestionsWithoutExposingAnswers() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 25, 10, 0);
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{101L, "{\"selected\":\"B\"}", "Module 1 - Part 5", 10L, time}
                ));
        when(userMistakeQueryRepository.findQuestionMetaByIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 5L, "Part 5", "Câu Part 5"}));
        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(anyCollection()))
                .thenReturn(Collections.emptyList());

        Question q101 = buildQuestion(101L, 5L, "Part 5", ESection.READING, "A",
                "{\"text\":\"Test stem\",\"options\":[{\"key\":\"A\",\"text\":\"opt A\"},{\"key\":\"B\",\"text\":\"opt B\"}]}",
                "{\"analysis\":\"Secret analysis\"}");
        when(questionBatchLoader.loadDetailedQuestionsMap(List.of(101L)))
                .thenReturn(Map.of(101L, q101));

        MistakeRetrySetResponse retrySet = service.getRetryQuestionSet(USER_ID, null, null, null, null, 20);

        assertThat(retrySet.getTotalAvailable()).isEqualTo(1);
        assertThat(retrySet.getSessionSize()).isEqualTo(1);
        assertThat(retrySet.getQuestions()).hasSize(1);
        assertThat(retrySet.getQuestions().get(0).getOrderNo()).isEqualTo(1);
        assertThat(retrySet.getQuestions().get(0).getOptions()).hasSize(2);
    }

    @Test
    void submitRetryAnswers_gradesOwnedQuestionsAndReturnsExplanations() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 25, 10, 0);
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(
                        new Object[]{101L, "{\"selected\":\"B\"}", "Module 1", 10L, time},
                        new Object[]{102L, "{\"selected\":\"D\"}", "Module 1", 10L, time}
                ));

        Question q101 = buildQuestion(101L, 5L, "Part 5", ESection.READING, "A", "{\"text\":\"Q1\"}", "{\"clue\":\"Clue 1\"}");
        Question q102 = buildQuestion(102L, 5L, "Part 5", ESection.READING, "C", "{\"text\":\"Q2\"}", "{\"clue\":\"Clue 2\"}");
        when(questionBatchLoader.loadDetailedQuestionsMap(anyCollection()))
                .thenReturn(Map.of(101L, q101, 102L, q102));

        SubmitMistakeRetryRequest request = SubmitMistakeRetryRequest.builder()
                .answers(List.of(
                        MistakeRetryAnswerItem.builder().questionId(101L).selectedAnswer("a").build(),
                        MistakeRetryAnswerItem.builder().questionId(102L).selectedAnswer("B").build()
                ))
                .build();

        MistakeRetrySubmitResponse response = service.submitRetryAnswers(USER_ID, request);

        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getAnsweredCount()).isEqualTo(2);
        assertThat(response.getCorrectCount()).isEqualTo(1);
        assertThat(response.getAccuracyPercent()).isEqualTo(50);
        assertThat(response.getResults().get(0).getIsCorrect()).isTrue();
        assertThat(response.getResults().get(0).getExplanation().getClue()).isEqualTo("Clue 1");
        assertThat(response.getResults().get(1).getIsCorrect()).isFalse();
        assertThat(response.getResults().get(1).getCorrectAnswer()).isEqualTo("C");
    }

    @Test
    void submitRetryAnswers_whenSubmittingQuestionNotInUserMistakes_throwsForbidden() {
        when(userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                eq(USER_ID), eq(ETestType.FULL_MOCK_TEST), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());

        SubmitMistakeRetryRequest request = SubmitMistakeRetryRequest.builder()
                .answers(List.of(
                        MistakeRetryAnswerItem.builder().questionId(999L).selectedAnswer("A").build()
                ))
                .build();

        assertThatThrownBy(() -> service.submitRetryAnswers(USER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(questionBatchLoader, never()).loadDetailedQuestionsMap(anyCollection());
    }

    private Question buildQuestion(
            Long id,
            Long partId,
            String partName,
            ESection section,
            String correctKey,
            String questionData,
            String explanation
    ) {
        Part part = new Part();
        part.setId(partId);
        part.setName(partName);
        part.setSections(section);

        Question q = new Question();
        q.setId(id);
        q.setName("Question " + id);
        q.setPart(part);
        q.setSections(section);
        q.setQuestionData(questionData);
        q.setCorrectAnswer("{\"key\":\"" + correctKey + "\"}");
        q.setExplanation(explanation);
        return q;
    }

    private Ability buildAbility(Long id, String name) {
        Ability ability = new Ability();
        ability.setId(id);
        ability.setName(name);
        return ability;
    }
}
