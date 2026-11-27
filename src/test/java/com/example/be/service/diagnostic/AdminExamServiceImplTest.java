package com.example.be.service.diagnostic;

import com.example.be.dto.request.diagnostic.AutoSelectQuestionsRequest;
import com.example.be.dto.response.diagnostic.AutoSelectQuestionsResponse;
import com.example.be.dto.response.diagnostic.ExamMatrixAvailabilityResponse;
import com.example.be.dto.response.diagnostic.ExamQuestionItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.diagnostic.ExamMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.service.diagnostic.implement.AdminExamServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExamServiceImplTest {

    @Mock
    private DiagnosticTestRepository diagnosticTestRepository;
    @Mock
    private DiagnosticAttemptRepository diagnosticAttemptRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private QuestionAbilityRepository questionAbilityRepository;
    @Mock
    private AbilityEvaluationRuleRepository abilityEvaluationRuleRepository;

    @Spy
    private ExamMapper examMapper = new ExamMapper();

    @InjectMocks
    private AdminExamServiceImpl adminExamService;

    @Test
    void getMatrixAvailability_shouldIncludeMinEvidenceCountFromRulesWithFallback() {
        Part part1 = createPart(1L, "Part 1", ESection.LISTENING);
        Part part2 = createPart(2L, "Part 2", ESection.LISTENING);
        Ability ab1 = createAbility(1L, "Human Actions", part1);
        Ability ab2 = createAbility(2L, "Wh-Questions", part2);

        AbilityEvaluationRule rule1 = new AbilityEvaluationRule();
        rule1.setAbility(ab1);
        rule1.setMinEvidenceCount(4);

        when(questionRepository.countPlayableQuestionsByPartId(anyLong())).thenReturn(10L);
        when(abilityEvaluationRuleRepository.findAllWithDetails()).thenReturn(List.of(rule1));
        when(abilityRepository.findAll()).thenReturn(List.of(ab1, ab2));
        when(questionAbilityRepository.countByAbilityId(1L)).thenReturn(8L);
        when(questionAbilityRepository.countByAbilityId(2L)).thenReturn(6L);

        ExamMatrixAvailabilityResponse result = adminExamService.getMatrixAvailability();

        assertThat(result.getAbilities()).hasSize(2);
        assertThat(result.getAbilities().get(0).getMinEvidenceCount()).isEqualTo(4);
        assertThat(result.getAbilities().get(0).getAvailableQuestionCount()).isEqualTo(8);
        assertThat(result.getAbilities().get(1).getMinEvidenceCount()).isEqualTo(3);
        assertThat(result.getAbilities().get(1).getAvailableQuestionCount()).isEqualTo(6);
    }

    @Test
    void autoSelectQuestions_diagnosticDefault48_shouldPickMinEvidencePerAbilityAndMapAbilities() {
        List<Ability> abilities = createSixteenAbilitiesAcrossSevenParts();
        List<AbilityEvaluationRule> rules = createRules(abilities, 3);
        List<Question> pool = createPlayablePool(abilities, 5);

        when(abilityRepository.findAll()).thenReturn(abilities);
        when(abilityEvaluationRuleRepository.findAllWithDetails()).thenReturn(rules);
        when(questionRepository.findAllPlayableWithAbilitiesAndPart()).thenReturn(pool);
        stubPoolLookup(pool);

        AutoSelectQuestionsRequest request = AutoSelectQuestionsRequest.builder()
                .testType(ETestType.DIAGNOSTIC)
                .build();

        AutoSelectQuestionsResponse response = adminExamService.autoSelectQuestions(request);

        assertThat(response.getTotalSelected()).isEqualTo(48);
        assertThat(response.getQuestions()).hasSize(48);
        assertThat(response.getQuestionIds()).doesNotHaveDuplicates();

        Map<Integer, Long> countByPart = response.getQuestions().stream()
                .collect(Collectors.groupingBy(ExamQuestionItemResponse::getPartNumber, Collectors.counting()));
        assertThat(countByPart).containsEntry(1, 6L)
                .containsEntry(2, 9L)
                .containsEntry(3, 6L)
                .containsEntry(4, 6L)
                .containsEntry(5, 9L)
                .containsEntry(6, 6L)
                .containsEntry(7, 6L);

        assertThat(response.getQuestions())
                .allSatisfy(item -> assertThat(item.getAbilityNames()).isNotEmpty());
    }

    @Test
    void autoSelectQuestions_diagnosticWith60Questions_shouldDistributeExtraAcrossParts() {
        List<Ability> abilities = createSixteenAbilitiesAcrossSevenParts();
        List<AbilityEvaluationRule> rules = createRules(abilities, 3);
        List<Question> pool = createPlayablePool(abilities, 8);

        when(abilityRepository.findAll()).thenReturn(abilities);
        when(abilityEvaluationRuleRepository.findAllWithDetails()).thenReturn(rules);
        when(questionRepository.findAllPlayableWithAbilitiesAndPart()).thenReturn(pool);
        stubPoolLookup(pool);

        AutoSelectQuestionsRequest request = AutoSelectQuestionsRequest.builder()
                .testType(ETestType.DIAGNOSTIC)
                .totalQuestions(60)
                .build();

        AutoSelectQuestionsResponse response = adminExamService.autoSelectQuestions(request);

        assertThat(response.getTotalSelected()).isEqualTo(60);
        Map<Integer, Long> countByPart = response.getQuestions().stream()
                .collect(Collectors.groupingBy(ExamQuestionItemResponse::getPartNumber, Collectors.counting()));
        assertThat(countByPart.keySet()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7);
        assertThat(countByPart.get(7)).isGreaterThan(6L);
    }

    @Test
    void autoSelectQuestions_diagnosticInsufficientAbilityQuestions_shouldThrow() {
        Part part1 = createPart(1L, "Part 1", ESection.LISTENING);
        Ability ab1 = createAbility(1L, "Human Actions", part1);
        AbilityEvaluationRule rule1 = new AbilityEvaluationRule();
        rule1.setAbility(ab1);
        rule1.setMinEvidenceCount(3);

        List<Question> pool = createPlayablePool(List.of(ab1), 2);

        when(abilityRepository.findAll()).thenReturn(List.of(ab1));
        when(abilityEvaluationRuleRepository.findAllWithDetails()).thenReturn(List.of(rule1));
        when(questionRepository.findAllPlayableWithAbilitiesAndPart()).thenReturn(pool);

        AutoSelectQuestionsRequest request = AutoSelectQuestionsRequest.builder()
                .testType(ETestType.DIAGNOSTIC)
                .totalQuestions(3)
                .build();

        assertThatThrownBy(() -> adminExamService.autoSelectQuestions(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.QUESTION_BANK_NOT_ENOUGH));
    }

    @Test
    void autoSelectQuestions_diagnosticBelowMinimumTarget_shouldSelectRequestedCount() {
        List<Ability> abilities = createSixteenAbilitiesAcrossSevenParts();
        List<AbilityEvaluationRule> rules = createRules(abilities, 3);
        List<Question> pool = createPlayablePool(abilities, 5);

        when(abilityRepository.findAll()).thenReturn(abilities);
        when(abilityEvaluationRuleRepository.findAllWithDetails()).thenReturn(rules);
        when(questionRepository.findAllPlayableWithAbilitiesAndPart()).thenReturn(pool);
        stubPoolLookup(pool);

        AutoSelectQuestionsRequest request = AutoSelectQuestionsRequest.builder()
                .testType(ETestType.DIAGNOSTIC)
                .totalQuestions(24)
                .build();

        AutoSelectQuestionsResponse response = adminExamService.autoSelectQuestions(request);

        assertThat(response.getTotalSelected()).isEqualTo(24);
        assertThat(response.getQuestionIds()).doesNotHaveDuplicates();
    }

    private Part createPart(Long id, String name, ESection section) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setSections(section);
        return part;
    }

    private Ability createAbility(Long id, String name, Part part) {
        Ability ability = new Ability();
        ability.setId(id);
        ability.setName(name);
        ability.setPart(part);
        ability.setSections(part.getSections());
        return ability;
    }

    private List<Ability> createSixteenAbilitiesAcrossSevenParts() {
        int[] abilitiesPerPart = {2, 3, 2, 2, 3, 2, 2};
        List<Ability> abilities = new ArrayList<>();
        long abilityId = 1L;
        for (int partNo = 1; partNo <= 7; partNo++) {
            ESection section = partNo <= 4 ? ESection.LISTENING : ESection.READING;
            Part part = createPart((long) partNo, "Part " + partNo, section);
            for (int i = 0; i < abilitiesPerPart[partNo - 1]; i++) {
                abilities.add(createAbility(abilityId, "Ability " + abilityId, part));
                abilityId++;
            }
        }
        return abilities;
    }

    private List<AbilityEvaluationRule> createRules(List<Ability> abilities, int minEvidenceCount) {
        List<AbilityEvaluationRule> rules = new ArrayList<>();
        for (Ability ab : abilities) {
            AbilityEvaluationRule rule = new AbilityEvaluationRule();
            rule.setAbility(ab);
            rule.setMinEvidenceCount(minEvidenceCount);
            rule.setDevelopingThreshold(50f);
            rule.setStableThreshold(75f);
            rules.add(rule);
        }
        return rules;
    }

    private List<Question> createPlayablePool(List<Ability> abilities, int questionsPerAbility) {
        List<Question> pool = new ArrayList<>();
        long qId = 1L;
        for (Ability ab : abilities) {
            for (int i = 0; i < questionsPerAbility; i++) {
                Question q = new Question();
                q.setId(qId);
                q.setName("Question " + qId);
                q.setPart(ab.getPart());
                q.setSections(ab.getSections());
                q.setDifficulty(3);

                QuestionAbility qa = new QuestionAbility();
                qa.setQuestion(q);
                qa.setAbility(ab);
                q.setQuestionAbilities(List.of(qa));

                pool.add(q);
                qId++;
            }
        }
        return pool;
    }

    private void stubPoolLookup(List<Question> pool) {
        when(questionRepository.findAllWithAbilitiesByIdIn(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return pool.stream().filter(q -> ids.contains(q.getId())).toList();
        });
    }
}
