package com.example.be.mapper.exam;

import com.example.be.dto.response.exam.PracticeRoomQuestionResponse;
import com.example.be.dto.response.exam.PracticeRoomResponse;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.mapper.question.ExamQuestionMapper.MappedQuestionContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyển đổi dữ liệu bài tập thực hành (Lesson + PracticeQuestion) sang DTO Phòng luyện tập (UC12).
 * Tái sử dụng {@link ExamQuestionMapper} để xử lý câu hỏi đơn, câu hỏi chùm, media và giải thích chi tiết.
 */
@Component
@RequiredArgsConstructor
public class PracticeRoomMapper {

    private static final String DEFAULT_PART_NAME = "Chuyên đề luyện tập";

    private final ExamQuestionMapper examQuestionMapper;

    /**
     * Ánh xạ bài học thực hành và danh sách câu hỏi đã nạp đầy đủ sang {@link PracticeRoomResponse}.
     *
     * @param lesson              Thực thể Lesson đã nạp Module, Part, Ability
     * @param orderedPracticeList Danh sách liên kết {@link PracticeQuestion}
     * @param detailedQuestionMap Bản đồ Question đã nạp sẵn Media và câu hỏi cha
     * @return {@link PracticeRoomResponse}
     */
    public PracticeRoomResponse toRoomResponse(
            Lesson lesson,
            List<PracticeQuestion> orderedPracticeList,
            Map<Long, Question> detailedQuestionMap
    ) {
        List<PracticeQuestion> safeOrdered = orderedPracticeList != null
                ? orderedPracticeList.stream()
                        .sorted(Comparator.comparing(PracticeQuestion::getOrderNo))
                        .toList()
                : Collections.emptyList();

        List<PracticeRoomQuestionResponse> questionResponses = new ArrayList<>(safeOrdered.size());
        for (PracticeQuestion pq : safeOrdered) {
            if (pq.getQuestion() == null) {
                continue;
            }
            Question detailed = detailedQuestionMap != null
                    ? detailedQuestionMap.getOrDefault(pq.getQuestion().getId(), pq.getQuestion())
                    : pq.getQuestion();
            questionResponses.add(toQuestionResponse(pq, detailed));
        }

        Module module = lesson.getModule();
        Part part = module != null ? module.getPart() : null;
        ESection section = part != null && part.getSections() != null
                ? part.getSections()
                : (!questionResponses.isEmpty() ? ESection.valueOf(questionResponses.get(0).getSection()) : ESection.READING);

        return PracticeRoomResponse.builder()
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .moduleId(module != null ? module.getId() : null)
                .moduleTitle(module != null ? module.getTitle() : "")
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(section.name())
                .abilityName(lesson.getAbility() != null ? lesson.getAbility().getName() : null)
                .totalQuestions(questionResponses.size())
                .isEmpty(questionResponses.isEmpty())
                .questions(questionResponses)
                .build();
    }

    /**
     * Ánh xạ một bản ghi {@link PracticeQuestion} sang {@link PracticeRoomQuestionResponse}.
     */
    public PracticeRoomQuestionResponse toQuestionResponse(PracticeQuestion pq, Question detailedQuestion) {
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(detailedQuestion, null);
        String correctKey = examQuestionMapper.extractAnswerKey(detailedQuestion.getCorrectAnswer());

        return PracticeRoomQuestionResponse.builder()
                .practiceQuestionId(pq.getId())
                .questionId(content.getQuestionId())
                .orderNo(pq.getOrderNo())
                .parentId(content.getParentId())
                .partId(content.getPartId())
                .partNo(content.getPartNo())
                .partName(content.getPartName())
                .section(content.getSection())
                .title(content.getTitle())
                .instruction(content.getInstruction())
                .passage(content.getPassage())
                .audioUrl(content.getAudioUrl())
                .imageUrl(content.getImageUrl())
                .isGroupQuestion(content.getIsGroupQuestion())
                .options(content.getOptions())
                .correctAnswer(correctKey)
                .explanation(examQuestionMapper.resolveExplanation(detailedQuestion))
                .build();
    }
}
