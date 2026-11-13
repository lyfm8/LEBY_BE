package com.example.be.mapper.exam;

import com.example.be.dto.request.exam.CreateExamRequest;
import com.example.be.dto.response.exam.ExamDetailResponse;
import com.example.be.dto.response.exam.ExamListItemResponse;
import com.example.be.dto.response.exam.ExamQuestionItemResponse;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EExamStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyển đổi giữa Entity DiagnosticTest / DiagnosticTestQuestion và các DTO Exam.
 */
@Component
public class ExamMapper {

    private static final int DEFAULT_POINTS_PER_QUESTION = 5;

    public ExamListItemResponse toListItemResponse(DiagnosticTest exam) {
        if (exam == null) {
            return null;
        }

        return ExamListItemResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .testType(exam.getTestType())
                .examStatus(exam.getExamStatus())
                .durationMinutes(exam.getDurationMinutes())
                .totalQuestions(exam.getTestQuestions() != null ? exam.getTestQuestions().size() : 0)
                .updatedAt(exam.getUpdatedAt())
                .updatedBy(exam.getUpdatedBy() != null ? exam.getUpdatedBy() : "Admin")
                .build();
    }

    public ExamDetailResponse toDetailResponse(DiagnosticTest exam, Map<Long, List<String>> questionAbilityMap) {
        if (exam == null) {
            return null;
        }

        List<DiagnosticTestQuestion> testQuestions = exam.getTestQuestions() != null
                ? new ArrayList<>(exam.getTestQuestions())
                : new ArrayList<>();

        testQuestions.sort(Comparator.comparing(DiagnosticTestQuestion::getOrderNo));

        Map<Integer, Integer> partDistribution = new HashMap<>();
        int listening = 0;
        int reading = 0;
        List<ExamQuestionItemResponse> questionResponses = new ArrayList<>();

        for (DiagnosticTestQuestion tq : testQuestions) {
            Question q = tq.getQuestion();
            int partNo = (q.getPart() != null && q.getPart().getId() != null) ? q.getPart().getId().intValue() : 1;
            partDistribution.put(partNo, partDistribution.getOrDefault(partNo, 0) + 1);

            if (q.getSections() == ESection.LISTENING || (partNo >= 1 && partNo <= 4)) {
                listening++;
            } else {
                reading++;
            }

            List<String> abilities = questionAbilityMap != null
                    ? questionAbilityMap.getOrDefault(q.getId(), Collections.emptyList())
                    : Collections.emptyList();

            questionResponses.add(toQuestionItemResponse(tq, q, partNo, abilities));
        }

        return ExamDetailResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .testType(exam.getTestType())
                .examStatus(exam.getExamStatus())
                .durationMinutes(exam.getDurationMinutes())
                .totalQuestions(testQuestions.size())
                .listeningCount(listening)
                .readingCount(reading)
                .partDistribution(partDistribution)
                .questions(questionResponses)
                .updatedAt(exam.getUpdatedAt())
                .updatedBy(exam.getUpdatedBy() != null ? exam.getUpdatedBy() : "Admin")
                .build();
    }

    public ExamQuestionItemResponse toQuestionItemResponse(
            DiagnosticTestQuestion tq,
            Question q,
            int partNo,
            List<String> abilities
    ) {
        return ExamQuestionItemResponse.builder()
                .id(tq.getId())
                .questionId(q.getId())
                .orderNo(tq.getOrderNo())
                .points(tq.getPoints())
                .questionCode("Q-P" + partNo + "-" + String.format("%04d", q.getId()))
                .questionName(q.getName())
                .partId(q.getPart() != null ? q.getPart().getId().intValue() : null)
                .partNumber(partNo)
                .partName(q.getPart() != null ? q.getPart().getName() : "Part " + partNo)
                .section(q.getSections())
                .difficulty(q.getDifficulty())
                .abilityNames(abilities)
                .build();
    }

    public List<ExamQuestionItemResponse> toPlayableQuestionItemResponses(List<Question> questions) {
        if (questions == null) {
            return Collections.emptyList();
        }

        List<ExamQuestionItemResponse> responses = new ArrayList<>();
        int order = 1;
        for (Question q : questions) {
            int partNo = (q.getPart() != null && q.getPart().getId() != null) ? q.getPart().getId().intValue() : 1;
            responses.add(ExamQuestionItemResponse.builder()
                    .questionId(q.getId())
                    .orderNo(order++)
                    .points(DEFAULT_POINTS_PER_QUESTION)
                    .questionCode("Q-P" + partNo + "-" + String.format("%04d", q.getId()))
                    .questionName(q.getName())
                    .partId(q.getPart() != null ? q.getPart().getId().intValue() : null)
                    .partNumber(partNo)
                    .partName(q.getPart() != null ? q.getPart().getName() : "Part " + partNo)
                    .section(q.getSections())
                    .difficulty(q.getDifficulty())
                    .build());
        }
        return responses;
    }

    public DiagnosticTest toEntity(CreateExamRequest request, String currentUsername, EExamStatus initialStatus) {
        if (request == null) {
            return null;
        }

        DiagnosticTest exam = new DiagnosticTest();
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setTestType(request.getTestType());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setExamStatus(initialStatus);
        exam.setUpdatedBy(currentUsername != null ? currentUsername : "Admin");

        return exam;
    }
}

