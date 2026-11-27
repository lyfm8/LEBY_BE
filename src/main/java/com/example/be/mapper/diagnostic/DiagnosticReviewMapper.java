package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewResponse;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.mapper.question.ExamQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyển đổi dữ liệu lượt thi chẩn đoán đã hoàn thành sang DTO phục vụ chế độ Xem lại bài thi (UC07 - BR14).
 * Tái sử dụng {@link DiagnosticRoomMapper} và {@link ExamQuestionMapper} để tránh trùng lặp logic ánh xạ câu hỏi và giải thích.
 */
@Component
public class DiagnosticReviewMapper {

    private final DiagnosticRoomMapper diagnosticRoomMapper;
    private final ExamQuestionMapper examQuestionMapper;

    @Autowired
    public DiagnosticReviewMapper(
            DiagnosticRoomMapper diagnosticRoomMapper,
            ExamQuestionMapper examQuestionMapper
    ) {
        this.diagnosticRoomMapper = diagnosticRoomMapper;
        this.examQuestionMapper = examQuestionMapper;
    }

    /**
     * Constructor dự phòng giữ tương thích cho các unit test khởi tạo trực tiếp với {@link ObjectMapper}.
     */
    public DiagnosticReviewMapper(DiagnosticRoomMapper diagnosticRoomMapper, ObjectMapper objectMapper) {
        this(diagnosticRoomMapper, new ExamQuestionMapper(objectMapper));
    }

    /**
     * Ánh xạ toàn bộ thông tin lượt thi đã nộp và danh sách câu hỏi/đáp án sang {@link DiagnosticReviewResponse}.
     */
    public DiagnosticReviewResponse toReviewResponse(
            DiagnosticTest test,
            DiagnosticAttempt attempt,
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, DiagnosticAnswer> answerByQuestionId
    ) {
        List<DiagnosticTestQuestion> safeOrdered = orderedQuestions != null
                ? orderedQuestions.stream().sorted(Comparator.comparing(DiagnosticTestQuestion::getOrderNo)).toList()
                : Collections.emptyList();

        List<DiagnosticReviewQuestionResponse> reviewQuestions = new ArrayList<>(safeOrdered.size());
        int correctCount = 0;

        for (DiagnosticTestQuestion testQuestion : safeOrdered) {
            Question question = detailedQuestionMap != null
                    ? detailedQuestionMap.getOrDefault(testQuestion.getQuestion().getId(), testQuestion.getQuestion())
                    : testQuestion.getQuestion();
            DiagnosticAnswer answer = answerByQuestionId != null
                    ? answerByQuestionId.get(question.getId())
                    : null;

            DiagnosticReviewQuestionResponse mapped = toReviewQuestionResponse(testQuestion, question, answer);
            if (Boolean.TRUE.equals(mapped.getIsCorrect())) {
                correctCount++;
            }
            reviewQuestions.add(mapped);
        }

        return DiagnosticReviewResponse.builder()
                .attemptId(attempt.getId())
                .testId(test != null ? test.getId() : null)
                .testTitle(test != null && test.getTitle() != null ? test.getTitle() : "Bài thi chẩn đoán năng lực")
                .completedAt(attempt.getCompletedAt())
                .totalQuestions(reviewQuestions.size())
                .correctCount(correctCount)
                .questions(reviewQuestions)
                .build();
    }

    /**
     * Ánh xạ một câu hỏi trong đề kèm câu trả lời của học viên sang {@link DiagnosticReviewQuestionResponse}.
     */
    public DiagnosticReviewQuestionResponse toReviewQuestionResponse(
            DiagnosticTestQuestion testQuestion,
            Question question,
            DiagnosticAnswer answer
    ) {
        DiagnosticQuestionResponse base = diagnosticRoomMapper.toQuestionResponse(testQuestion, question);

        String correctKey = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
        String userKey = answer != null ? examQuestionMapper.extractAnswerKey(answer.getUserAnswer()) : null;
        boolean isCorrect = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());
        boolean isFlagged = answer != null && Boolean.TRUE.equals(answer.getIsFlagged());

        QuestionExplanationResponse explanation = examQuestionMapper.resolveExplanation(question);

        return DiagnosticReviewQuestionResponse.builder()
                .questionId(base.getQuestionId())
                .orderNo(base.getOrderNo())
                .parentId(base.getParentId())
                .partId(base.getPartId())
                .partNo(base.getPartNo())
                .partName(base.getPartName())
                .section(base.getSection())
                .title(base.getTitle())
                .instruction(base.getInstruction())
                .passage(base.getPassage())
                .audioUrl(base.getAudioUrl())
                .imageUrl(base.getImageUrl())
                .isGroupQuestion(base.getIsGroupQuestion())
                .options(base.getOptions())
                .userAnswer(userKey)
                .correctAnswer(correctKey)
                .isCorrect(isCorrect)
                .isFlagged(isFlagged)
                .explanation(explanation)
                .build();
    }

    /**
     * Trích xuất ký hiệu đáp án (A/B/C/D) từ chuỗi JSON hoặc chuỗi ký tự đơn.
     */
    public String extractAnswerKey(String jsonOrKey) {
        return examQuestionMapper.extractAnswerKey(jsonOrKey);
    }
}
