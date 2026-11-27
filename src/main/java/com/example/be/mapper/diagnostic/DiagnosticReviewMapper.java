package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewResponse;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.dto.response.question.QuestionVocabularyItemResponse;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mapper chuyển đổi dữ liệu lượt thi chẩn đoán đã hoàn thành sang DTO phục vụ chế độ Xem lại bài thi (UC07 - BR14).
 * Tái sử dụng {@link DiagnosticRoomMapper} để ánh xạ thông tin câu hỏi, media và bài đọc,
 * đồng thời bổ sung đáp án đã chọn, đáp án đúng, trạng thái đúng/sai và lời giải chi tiết.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosticReviewMapper {

    private final DiagnosticRoomMapper diagnosticRoomMapper;
    private final ObjectMapper objectMapper;

    /**
     * Ánh xạ toàn bộ thông tin lượt thi đã nộp và danh sách câu hỏi/đáp án sang {@link DiagnosticReviewResponse}.
     *
     * @param test                Đề thi chẩn đoán gốc
     * @param attempt             Lượt thi đã hoàn thành
     * @param orderedQuestions    Danh sách liên kết câu hỏi trong đề đã sắp xếp theo orderNo
     * @param detailedQuestionMap Bản đồ câu hỏi đã nạp sẵn Media và câu hỏi cha
     * @param answerByQuestionId  Bản đồ câu trả lời của học viên theo questionId
     * @return {@link DiagnosticReviewResponse} đầy đủ dữ liệu cho màn hình Review Mode
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
     *
     * @param testQuestion Bản ghi liên kết đề thi - câu hỏi chứa orderNo
     * @param question     Thực thể câu hỏi đã nạp sẵn Part, Parent, Media
     * @param answer       Bản ghi câu trả lời của học viên (có thể null nếu học viên bỏ trống)
     * @return {@link DiagnosticReviewQuestionResponse}
     */
    public DiagnosticReviewQuestionResponse toReviewQuestionResponse(
            DiagnosticTestQuestion testQuestion,
            Question question,
            DiagnosticAnswer answer
    ) {
        // Tái sử dụng logic ánh xạ ngữ cảnh câu hỏi, passage, media và phương án từ DiagnosticRoomMapper
        DiagnosticQuestionResponse base = diagnosticRoomMapper.toQuestionResponse(testQuestion, question);

        String correctKey = normalizeOptionKey(extractAnswerKey(question.getCorrectAnswer()));
        String userKey = answer != null ? normalizeOptionKey(extractAnswerKey(answer.getUserAnswer())) : null;
        boolean isCorrect = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());
        boolean isFlagged = answer != null && Boolean.TRUE.equals(answer.getIsFlagged());

        QuestionExplanationResponse explanation = resolveExplanation(question);

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
        if (jsonOrKey == null) {
            return null;
        }
        String trimmed = jsonOrKey.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        JsonNode node = parseJsonNode(trimmed);
        if (node == null) {
            return null;
        }
        if (node.has("key") && !node.get("key").isNull()) {
            String val = node.get("key").asText("").trim();
            if (!val.isEmpty()) {
                return val;
            }
        }
        if (node.has("selected") && !node.get("selected").isNull()) {
            String val = node.get("selected").asText("").trim();
            if (!val.isEmpty()) {
                return val;
            }
        }
        return null;
    }

    /**
     * Hợp nhất giải thích của câu hỏi con với transcript/translation ở câu hỏi cha (nếu câu con chưa lưu riêng).
     */
    private QuestionExplanationResponse resolveExplanation(Question question) {
        QuestionExplanationResponse own = parseExplanation(question.getExplanation());
        Question parent = question.getParent();
        QuestionExplanationResponse parentExpl = parent != null ? parseExplanation(parent.getExplanation()) : null;

        String transcript = firstNonBlank(
                own != null ? own.getTranscript() : null,
                parentExpl != null ? parentExpl.getTranscript() : null
        );
        String translation = firstNonBlank(
                own != null ? own.getTranslation() : null,
                parentExpl != null ? parentExpl.getTranslation() : null
        );
        String clue = own != null ? cleanBlankToNull(own.getClue()) : null;
        String analysis = own != null ? cleanBlankToNull(own.getAnalysis()) : null;
        List<QuestionVocabularyItemResponse> vocabulary = own != null && own.getVocabulary() != null
                ? own.getVocabulary()
                : Collections.emptyList();

        boolean hasAnyContent = transcript != null
                || translation != null
                || clue != null
                || analysis != null
                || !vocabulary.isEmpty();

        if (!hasAnyContent) {
            return null;
        }

        return QuestionExplanationResponse.builder()
                .transcript(transcript)
                .translation(translation)
                .clue(clue)
                .analysis(analysis)
                .vocabulary(vocabulary)
                .build();
    }

    private QuestionExplanationResponse parseExplanation(String explanationJson) {
        JsonNode node = parseJsonNode(explanationJson);
        if (node == null || node.isNull()) {
            return null;
        }

        List<QuestionVocabularyItemResponse> vocabulary = new ArrayList<>();
        if (node.has("vocabulary") && node.get("vocabulary").isArray()) {
            for (JsonNode vNode : node.get("vocabulary")) {
                String word = extractText(vNode, "word");
                String type = extractText(vNode, "type");
                String meaning = extractText(vNode, "meaning");
                if (word != null || meaning != null) {
                    vocabulary.add(QuestionVocabularyItemResponse.builder()
                            .word(word != null ? word : "")
                            .type(type != null ? type : "")
                            .meaning(meaning != null ? meaning : "")
                            .build());
                }
            }
        }

        return QuestionExplanationResponse.builder()
                .transcript(extractText(node, "transcript"))
                .translation(extractText(node, "translation"))
                .clue(extractText(node, "clue"))
                .analysis(extractText(node, "analysis"))
                .vocabulary(vocabulary)
                .build();
    }

    private JsonNode parseJsonNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.warn("Không thể parse JSON trong DiagnosticReviewMapper: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return cleanBlankToNull(node.get(fieldName).asText(""));
    }

    private String cleanBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = cleanBlankToNull(primary);
        if (first != null) {
            return first;
        }
        return cleanBlankToNull(fallback);
    }

    private String normalizeOptionKey(String key) {
        String cleaned = cleanBlankToNull(key);
        return cleaned != null ? cleaned.toUpperCase(Locale.ROOT) : null;
    }
}
