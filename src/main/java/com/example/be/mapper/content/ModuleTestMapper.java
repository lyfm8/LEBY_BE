package com.example.be.mapper.content;

import com.example.be.dto.response.content.ModuleTestConfigDetailResponse;
import com.example.be.dto.response.content.ModuleTestOverviewItemResponse;
import com.example.be.dto.response.content.ModuleTestQuestionItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleTestSelectionMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mapper chuyên biệt chuyển đổi Entity sang DTO cho nghiệp vụ Cấu hình Bài thi vượt ải (UC A07).
 */
@Component
public class ModuleTestMapper {

    /**
     * Chuyển đổi Module sang dòng tổng quan trạng thái bài thi vượt ải.
     *
     * @param module               Thực thể Module (đã nạp sẵn part, moduleAbilities, lessons)
     * @param currentQuestionCount Số lượng câu hỏi đã gán trong module_test_questions
     * @return ModuleTestOverviewItemResponse
     */
    public ModuleTestOverviewItemResponse toOverviewItem(Module module, int currentQuestionCount) {
        if (module == null) {
            return null;
        }

        List<Long> abilityIds = new ArrayList<>();
        List<String> abilityNames = new ArrayList<>();
        extractModuleAbilities(module, abilityIds, abilityNames);

        int totalLessons = module.getLessons() != null ? module.getLessons().size() : 0;
        int totalVideoLessons = countVideoLessons(module.getLessons());
        EModuleTestSelectionMode mode = resolveSelectionMode(module);
        // Coi là đã cấu hình đề nếu đang dùng chế độ RANDOM_BY_ABILITY (có số câu > 0) hoặc đã gán ít nhất 1 câu thủ công
        boolean hasTestConfigured = mode == EModuleTestSelectionMode.RANDOM_BY_ABILITY
                ? (module.getTestQuestionCount() != null && module.getTestQuestionCount() > 0)
                : currentQuestionCount > 0;

        Part part = module.getPart();
        return ModuleTestOverviewItemResponse.builder()
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .sequence(module.getSequence())
                .moduleStatus(module.getStatus())
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .abilityIds(abilityIds)
                .abilityNames(abilityNames)
                .totalLessons(totalLessons)
                .totalVideoLessons(totalVideoLessons)
                .defaultPassScore(module.getDefaultPassScore())
                .requiredCompletionRate(module.getRequiredCompletionRate())
                .durationMinutes(module.getTestDurationMinutes() != null ? module.getTestDurationMinutes() : 25)
                .questionCount(module.getTestQuestionCount() != null ? module.getTestQuestionCount() : 25)
                .currentQuestionCount(currentQuestionCount)
                .shuffleQuestions(module.getShuffleQuestions() != null ? module.getShuffleQuestions() : true)
                .selectionMode(mode)
                .hasTestConfigured(hasTestConfigured)
                .hasNoLessonsWarning(totalLessons == 0)
                .build();
    }

    /**
     * Chuyển đổi Module kèm danh sách câu hỏi vượt ải sang DTO chi tiết cấu hình.
     *
     * @param module                        Thực thể Module
     * @param questions                     Danh sách câu hỏi đã gắn vào đề vượt ải
     * @param availableAbilityQuestionCount Số câu hỏi khả dụng thuộc Ability của Module trong kho
     * @return ModuleTestConfigDetailResponse
     */
    public ModuleTestConfigDetailResponse toConfigDetail(
            Module module,
            List<ModuleTestQuestionItemResponse> questions,
            long availableAbilityQuestionCount
    ) {
        if (module == null) {
            return null;
        }

        List<Long> abilityIds = new ArrayList<>();
        List<String> abilityNames = new ArrayList<>();
        extractModuleAbilities(module, abilityIds, abilityNames);

        int totalLessons = module.getLessons() != null ? module.getLessons().size() : 0;
        int totalVideoLessons = countVideoLessons(module.getLessons());
        List<ModuleTestQuestionItemResponse> safeQuestions = questions != null ? questions : Collections.emptyList();
        EModuleTestSelectionMode mode = resolveSelectionMode(module);
        boolean hasTestConfigured = mode == EModuleTestSelectionMode.RANDOM_BY_ABILITY
                ? (module.getTestQuestionCount() != null && module.getTestQuestionCount() > 0)
                : !safeQuestions.isEmpty();

        Part part = module.getPart();
        return ModuleTestConfigDetailResponse.builder()
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .sequence(module.getSequence())
                .moduleStatus(module.getStatus())
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .section(part != null ? part.getSections() : null)
                .abilityIds(abilityIds)
                .abilityNames(abilityNames)
                .totalLessons(totalLessons)
                .totalVideoLessons(totalVideoLessons)
                .defaultPassScore(module.getDefaultPassScore())
                .requiredCompletionRate(module.getRequiredCompletionRate())
                .durationMinutes(module.getTestDurationMinutes() != null ? module.getTestDurationMinutes() : 25)
                .questionCount(module.getTestQuestionCount() != null ? module.getTestQuestionCount() : 25)
                .currentQuestionCount(safeQuestions.size())
                .shuffleQuestions(module.getShuffleQuestions() != null ? module.getShuffleQuestions() : true)
                .selectionMode(mode)
                .availableAbilityQuestionCount(availableAbilityQuestionCount)
                .hasTestConfigured(hasTestConfigured)
                .hasNoLessonsWarning(totalLessons == 0)
                .questions(safeQuestions)
                .build();
    }

    /**
     * Chuyển đổi một bản ghi ModuleTestQuestion sang ModuleTestQuestionItemResponse.
     */
    public ModuleTestQuestionItemResponse toQuestionItem(
            ModuleTestQuestion mtq,
            String questionSummary,
            List<Long> abilityIds,
            List<String> abilityNames,
            boolean isMatchingAbility,
            boolean hasAttemptHistory
    ) {
        if (mtq == null || mtq.getQuestion() == null) {
            return null;
        }
        Question q = mtq.getQuestion();
        Part part = q.getPart();
        Question parent = q.getParent();

        return ModuleTestQuestionItemResponse.builder()
                .id(mtq.getId())
                .questionId(q.getId())
                .orderNo(mtq.getOrderNo())
                .points(mtq.getPoints())
                .questionName(q.getName())
                .questionSummary(questionSummary)
                .questionType(q.getType())
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .section(q.getSections() != null ? q.getSections() : (part != null ? part.getSections() : null))
                .difficulty(q.getDifficulty())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .abilityIds(abilityIds != null ? abilityIds : Collections.emptyList())
                .abilityNames(abilityNames != null ? abilityNames : Collections.emptyList())
                .isMatchingAbility(isMatchingAbility)
                .hasAttemptHistory(hasAttemptHistory)
                .build();
    }

    /**
     * Chuyển đổi trực tiếp một Question (chưa lưu vào module_test_questions, ví dụ kết quả auto-select) sang Item DTO.
     */
    public ModuleTestQuestionItemResponse toTransientQuestionItem(
            Question q,
            int orderNo,
            int defaultPoints,
            String questionSummary,
            List<Long> abilityIds,
            List<String> abilityNames,
            Set<Long> moduleAbilityIds
    ) {
        if (q == null) {
            return null;
        }
        Part part = q.getPart();
        Question parent = q.getParent();
        boolean isMatching = moduleAbilityIds == null || moduleAbilityIds.isEmpty()
                || (abilityIds != null && abilityIds.stream().anyMatch(moduleAbilityIds::contains));

        return ModuleTestQuestionItemResponse.builder()
                .id(null)
                .questionId(q.getId())
                .orderNo(orderNo)
                .points(defaultPoints)
                .questionName(q.getName())
                .questionSummary(questionSummary)
                .questionType(q.getType())
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .section(q.getSections() != null ? q.getSections() : (part != null ? part.getSections() : null))
                .difficulty(q.getDifficulty())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .abilityIds(abilityIds != null ? abilityIds : Collections.emptyList())
                .abilityNames(abilityNames != null ? abilityNames : Collections.emptyList())
                .isMatchingAbility(isMatching)
                .hasAttemptHistory(false)
                .build();
    }

    private void extractModuleAbilities(Module module, List<Long> abilityIds, List<String> abilityNames) {
        if (module.getModuleAbilities() == null) {
            return;
        }
        for (ModuleAbility ma : module.getModuleAbilities()) {
            Ability ability = ma.getAbility();
            if (ability != null) {
                abilityIds.add(ability.getId());
                abilityNames.add(ability.getName());
            }
        }
    }

    private int countVideoLessons(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Lesson lesson : lessons) {
            if (lesson.getLessonType() == ELessonType.VIDEO) {
                count++;
            }
        }
        return count;
    }

    private EModuleTestSelectionMode resolveSelectionMode(Module module) {
        return module.getTestSelectionMode() != null
                ? module.getTestSelectionMode()
                : EModuleTestSelectionMode.MANUAL;
    }
}
