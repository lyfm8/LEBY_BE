package com.example.be.service.content.implement;

import com.example.be.dto.request.content.AutoSelectModuleTestQuestionsRequest;
import com.example.be.dto.request.content.SaveModuleTestConfigRequest;
import com.example.be.dto.response.content.ModuleTestConfigDetailResponse;
import com.example.be.dto.response.content.ModuleTestOverviewItemResponse;
import com.example.be.dto.response.content.ModuleTestQuestionItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.ModuleTestMapper;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAnswerRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminModuleTestServiceImpl (Use Case A07 - Cấu hình Bài thi vượt ải mô-đun).
 */
@ExtendWith(MockitoExtension.class)
class AdminModuleTestServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private ModuleTestQuestionRepository moduleTestQuestionRepository;

    @Mock
    private ModuleTestAnswerRepository moduleTestAnswerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionAbilityRepository questionAbilityRepository;

    private AdminModuleTestServiceImpl service;

    @BeforeEach
    void setUp() {
        ModuleTestMapper mapper = new ModuleTestMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AdminModuleTestServiceImpl(
                moduleRepository,
                moduleTestQuestionRepository,
                moduleTestAnswerRepository,
                questionRepository,
                questionAbilityRepository,
                mapper,
                objectMapper
        );
    }

    @Test
    @DisplayName("Lấy danh sách tổng quan bài thi vượt ải trả về đúng trạng thái đã có đề và cảnh báo thiếu bài giảng (3b)")
    void getAllModuleTestOverviews_returnsCorrectStatusAndWarning() {
        Module module = buildModule(1L, 10L, "Part 1 - Mô tả tranh", 100L, "Nghe động từ hành động");
        when(moduleRepository.findAllWithLessonsAndAbilities()).thenReturn(List.of(module));
        when(moduleTestQuestionRepository.countQuestionsGroupedByModuleIds(List.of(1L)))
                .thenReturn(Collections.singletonList(new Object[]{1L, 5L}));

        List<ModuleTestOverviewItemResponse> result = service.getAllModuleTestOverviews();

        assertThat(result).hasSize(1);
        ModuleTestOverviewItemResponse item = result.get(0);
        assertThat(item.getModuleId()).isEqualTo(1L);
        assertThat(item.getCurrentQuestionCount()).isEqualTo(5);
        assertThat(item.isHasTestConfigured()).isTrue();
        assertThat(item.isHasNoLessonsWarning()).isTrue();
    }

    @Test
    @DisplayName("Lưu cấu hình chế độ RANDOM_BY_ABILITY báo lỗi 6c khi kho câu hỏi không đủ")
    void saveModuleTestConfig_randomModeNotEnoughQuestions_throwsQuestionBankNotEnough() {
        Module module = buildModule(1L, 10L, "Part 5", 100L, "Từ loại");
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(questionRepository.countPlayableQuestionsByAbilityIds(Set.of(100L))).thenReturn(8L);

        SaveModuleTestConfigRequest request = SaveModuleTestConfigRequest.builder()
                .durationMinutes(25)
                .questionCount(20)
                .shuffleQuestions(true)
                .selectionMode(EModuleTestSelectionMode.RANDOM_BY_ABILITY)
                .build();

        assertThatThrownBy(() -> service.saveModuleTestConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(ErrorCode.QUESTION_BANK_NOT_ENOUGH);
                    assertThat(baseEx.getMessage()).contains("chỉ có 8 câu");
                });
    }

    @Test
    @DisplayName("Lưu cấu hình MANUAL báo cảnh báo sư phạm 6b khi có câu hỏi lệch Ability và chưa xác nhận")
    void saveModuleTestConfig_manualMismatchAbilityWithoutConfirm_throwsAbilityMismatch() {
        Module module = buildModule(1L, 10L, "Part 5", 100L, "Từ loại");
        Question mismatchQuestion = buildQuestion(50L, "Câu hỏi lệch năng lực", null);
        QuestionAbility qaOther = buildQuestionAbility(mismatchQuestion, 999L, "Năng lực khác");

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(questionRepository.findAllById(List.of(50L))).thenReturn(List.of(mismatchQuestion));
        when(questionRepository.findByParentIdInOrderByOrderNoAsc(List.of(50L))).thenReturn(Collections.emptyList());
        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(List.of(50L))).thenReturn(List.of(qaOther));

        SaveModuleTestConfigRequest request = SaveModuleTestConfigRequest.builder()
                .durationMinutes(20)
                .questionCount(1)
                .shuffleQuestions(true)
                .selectionMode(EModuleTestSelectionMode.MANUAL)
                .questionIds(List.of(50L))
                .confirmMismatchAbility(false)
                .build();

        assertThatThrownBy(() -> service.saveModuleTestConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MODULE_TEST_ABILITY_MISMATCH));
    }

    @Test
    @DisplayName("Lưu cấu hình MANUAL tự động bóc tách câu hỏi chùm (Part 3, 4, 6, 7) thành các câu con playable")
    void saveModuleTestConfig_manualWithClusterParent_expandsToSubQuestions() {
        Module module = buildModule(1L, 3L, "Part 3", 100L, "Ý chính hội thoại");
        Question parent = buildQuestion(200L, "Đoạn hội thoại 1", null);
        Question child1 = buildQuestion(201L, "Câu con 1", parent);
        Question child2 = buildQuestion(202L, "Câu con 2", parent);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(questionRepository.findAllById(List.of(200L))).thenReturn(List.of(parent));
        when(questionRepository.findByParentIdInOrderByOrderNoAsc(List.of(200L))).thenReturn(List.of(child1, child2));
        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(anyList())).thenReturn(List.of(
                buildQuestionAbility(child1, 100L, "Ý chính hội thoại"),
                buildQuestionAbility(child2, 100L, "Ý chính hội thoại")
        ));
        when(moduleTestQuestionRepository.findByModuleIdWithQuestionOrderByOrderNoAsc(1L))
                .thenReturn(Collections.emptyList());
        when(questionRepository.countPlayableQuestionsByAbilityIds(Set.of(100L))).thenReturn(10L);

        SaveModuleTestConfigRequest request = SaveModuleTestConfigRequest.builder()
                .durationMinutes(25)
                .questionCount(10)
                .shuffleQuestions(true)
                .selectionMode(EModuleTestSelectionMode.MANUAL)
                .questionIds(List.of(200L))
                .confirmMismatchAbility(false)
                .build();

        ModuleTestConfigDetailResponse response = service.saveModuleTestConfig(1L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ModuleTestQuestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(moduleTestQuestionRepository).saveAll(captor.capture());
        List<ModuleTestQuestion> saved = captor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getQuestion().getId()).isEqualTo(201L);
        assertThat(saved.get(0).getOrderNo()).isEqualTo(1);
        assertThat(saved.get(1).getQuestion().getId()).isEqualTo(202L);
        assertThat(saved.get(1).getOrderNo()).isEqualTo(2);
        assertThat(response.getDurationMinutes()).isEqualTo(25);
        // Chế độ MANUAL tự động đồng bộ testQuestionCount theo đúng số câu thực tế sau bóc tách chùm
        assertThat(module.getTestQuestionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Chặn gỡ câu hỏi đã có lịch sử làm bài của học viên để bảo vệ khóa ngoại module_test_answers")
    void saveModuleTestConfig_removingQuestionWithAttemptHistory_throwsExamInUse() {
        Module module = buildModule(1L, 5L, "Part 5", 100L, "Từ loại");
        Question oldQuestion = buildQuestion(10L, "Câu cũ đã thi", null);
        Question newQuestion = buildQuestion(20L, "Câu mới", null);

        ModuleTestQuestion existingLink = new ModuleTestQuestion();
        existingLink.setId(500L);
        existingLink.setModule(module);
        existingLink.setQuestion(oldQuestion);
        existingLink.setOrderNo(1);
        existingLink.setPoints(10);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(questionRepository.findAllById(List.of(20L))).thenReturn(List.of(newQuestion));
        when(questionRepository.findByParentIdInOrderByOrderNoAsc(List.of(20L))).thenReturn(Collections.emptyList());
        when(moduleTestQuestionRepository.findByModuleIdWithQuestionOrderByOrderNoAsc(1L))
                .thenReturn(List.of(existingLink));
        when(moduleTestAnswerRepository.existsByModuleTestQuestionIdIn(anyCollection())).thenReturn(true);

        SaveModuleTestConfigRequest request = SaveModuleTestConfigRequest.builder()
                .durationMinutes(25)
                .questionCount(1)
                .shuffleQuestions(true)
                .selectionMode(EModuleTestSelectionMode.MANUAL)
                .questionIds(List.of(20L))
                .confirmMismatchAbility(true)
                .build();

        assertThatThrownBy(() -> service.saveModuleTestConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXAM_IN_USE));
    }

    @Test
    @DisplayName("Tự động chọn câu hỏi ngẫu nhiên (Luồng 6a) trả về đủ số câu yêu cầu khi kho đủ")
    void autoSelectQuestions_enoughQuestionsInPool_returnsRequestedCount() {
        Module module = buildModule(1L, 5L, "Part 5", 100L, "Từ loại");
        Question q1 = buildQuestion(1L, "Câu 1", null);
        Question q2 = buildQuestion(2L, "Câu 2", null);
        Question q3 = buildQuestion(3L, "Câu 3", null);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));
        when(questionRepository.findPlayableQuestionsByAbilityIds(Set.of(100L))).thenReturn(List.of(q1, q2, q3));
        when(questionAbilityRepository.findWithAbilityByQuestionIdIn(anyList())).thenReturn(List.of(
                buildQuestionAbility(q1, 100L, "Từ loại"),
                buildQuestionAbility(q2, 100L, "Từ loại"),
                buildQuestionAbility(q3, 100L, "Từ loại")
        ));

        AutoSelectModuleTestQuestionsRequest request = AutoSelectModuleTestQuestionsRequest.builder()
                .questionCount(2)
                .onlyMatchingAbilities(true)
                .build();

        List<ModuleTestQuestionItemResponse> picked = service.autoSelectQuestions(1L, request);

        assertThat(picked).hasSize(2);
        assertThat(picked).allMatch(ModuleTestQuestionItemResponse::isMatchingAbility);
    }

    @Test
    @DisplayName("Lưu cấu hình báo lỗi 5a khi thời gian hoặc số câu hỏi nhỏ hơn hoặc bằng 0")
    void saveModuleTestConfig_nonPositiveDurationOrCount_throwsBadRequest() {
        Module module = buildModule(1L, 5L, "Part 5", 100L, "Từ loại");
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(module));

        SaveModuleTestConfigRequest request = SaveModuleTestConfigRequest.builder()
                .durationMinutes(0)
                .questionCount(25)
                .shuffleQuestions(true)
                .selectionMode(EModuleTestSelectionMode.RANDOM_BY_ABILITY)
                .build();

        assertThatThrownBy(() -> service.saveModuleTestConfig(1L, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getMessage())
                        .contains("lớn hơn 0"));
    }

    // ==========================================
    // TEST FIXTURE HELPERS
    // ==========================================

    private Module buildModule(Long id, Long partId, String partName, Long abilityId, String abilityName) {
        Part part = new Part();
        part.setId(partId);
        part.setName(partName);

        Ability ability = new Ability();
        ability.setId(abilityId);
        ability.setName(abilityName);
        ability.setPart(part);

        Module module = new Module();
        module.setId(id);
        module.setTitle("Module Test " + id);
        module.setType("THEORY");
        module.setSequence(1);
        module.setStatus(EModuleStatus.PUBLISHED);
        module.setDefaultPassScore(65);
        module.setRequiredCompletionRate(80);
        module.setTestDurationMinutes(25);
        module.setTestQuestionCount(25);
        module.setShuffleQuestions(true);
        module.setTestSelectionMode(EModuleTestSelectionMode.MANUAL);
        module.setPart(part);

        ModuleAbility ma = new ModuleAbility();
        ma.setModule(module);
        ma.setAbility(ability);
        module.setModuleAbilities(List.of(ma));

        return module;
    }

    private Question buildQuestion(Long id, String name, Question parent) {
        Question q = new Question();
        q.setId(id);
        q.setName(name);
        q.setDifficulty(2);
        q.setParent(parent);
        q.setQuestionData("{\"text\":\"Sample question text\"}");
        return q;
    }

    private QuestionAbility buildQuestionAbility(Question question, Long abilityId, String abilityName) {
        Ability ability = new Ability();
        ability.setId(abilityId);
        ability.setName(abilityName);

        QuestionAbility qa = new QuestionAbility();
        qa.setQuestion(question);
        qa.setAbility(ability);
        return qa;
    }
}
