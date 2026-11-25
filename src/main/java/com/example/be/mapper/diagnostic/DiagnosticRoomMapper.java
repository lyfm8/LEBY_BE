package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticOptionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaKind;
import com.example.be.service.question.util.MediaKindResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapper chuyển đổi dữ liệu Đề thi và Câu hỏi sang các DTO phục vụ Phòng thi chẩn đoán (UC06).
 * Tuyệt đối không trả ra trường đáp án đúng ({@code correctAnswer}) hay lời giải ({@code explanation})
 * nhằm bảo đảm tính bảo mật và công bằng trong quá trình thi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosticRoomMapper {

    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("Part\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int PART_READING_PASSAGE_START = 6;

    private final ObjectMapper objectMapper;

    /**
     * Ánh xạ lượt làm bài chẩn đoán và danh sách câu hỏi đã nạp chi tiết sang DTO phòng thi.
     *
     * @param test                Đề thi chẩn đoán
     * @param attempt             Lượt làm bài hiện tại của học viên
     * @param orderedQuestions    Danh sách câu hỏi của đề đã sắp xếp theo orderNo
     * @param detailedQuestionMap Map câu hỏi đã nạp sẵn Media & câu hỏi cha
     * @param remainingSeconds    Thời gian còn lại của phiên làm bài (tính theo giây)
     * @return {@link DiagnosticRoomResponse} đầy đủ dữ liệu render phòng thi
     */
    public DiagnosticRoomResponse toRoomResponse(
            DiagnosticTest test,
            DiagnosticAttempt attempt,
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            long remainingSeconds
    ) {
        List<DiagnosticTestQuestion> safeOrdered = orderedQuestions != null
                ? orderedQuestions.stream().sorted(Comparator.comparing(DiagnosticTestQuestion::getOrderNo)).toList()
                : Collections.emptyList();

        List<DiagnosticQuestionResponse> questionResponses = new ArrayList<>(safeOrdered.size());
        for (DiagnosticTestQuestion tq : safeOrdered) {
            Question detailed = detailedQuestionMap != null
                    ? detailedQuestionMap.getOrDefault(tq.getQuestion().getId(), tq.getQuestion())
                    : tq.getQuestion();
            questionResponses.add(toQuestionResponse(tq, detailed));
        }

        Long targetProfileId = attempt.getTargetProfile() != null
                ? attempt.getTargetProfile().getId()
                : (attempt.getUser() != null && attempt.getUser().getTargetProfile() != null
                        ? attempt.getUser().getTargetProfile().getId()
                        : null);

        return DiagnosticRoomResponse.builder()
                .attemptId(attempt.getId())
                .testId(test.getId())
                .targetProfileId(targetProfileId)
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .startedAt(attempt.getStartedAt())
                .remainingSeconds(Math.max(0L, remainingSeconds))
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .build();
    }

    /**
     * Ánh xạ một mục câu hỏi trong đề thi sang DTO hiển thị trong phòng thi.
     * Tự động hợp nhất ngữ cảnh bài đọc, ảnh minh họa và audio từ câu hỏi cha nếu là câu hỏi chùm.
     *
     * @param testQuestion Bản ghi liên kết đề thi - câu hỏi (chứa orderNo)
     * @param question     Thực thể câu hỏi đã nạp đầy đủ quan hệ Part, Parent, Media
     * @return {@link DiagnosticQuestionResponse}
     */
    public DiagnosticQuestionResponse toQuestionResponse(DiagnosticTestQuestion testQuestion, Question question) {
        Part part = question.getPart();
        int partNo = resolvePartNumber(part);
        ESection section = resolveSection(question, part, partNo);

        Question parent = question.getParent();
        JsonNode qData = parseJsonNode(question.getQuestionData());
        JsonNode parentData = parent != null ? parseJsonNode(parent.getQuestionData()) : null;

        String title = firstNonBlank(extractText(qData, "text"), question.getName());
        String instruction = resolveInstruction(qData, parentData);
        String passage = resolvePassage(partNo, section, qData, parentData);

        String audioUrl = resolveMediaUrl(question, parent, qData, parentData, EMediaKind.AUDIO, "audioUrl");
        String imageUrl = resolveMediaUrl(question, parent, qData, parentData, EMediaKind.IMAGE, "imageUrl");

        return DiagnosticQuestionResponse.builder()
                .questionId(question.getId())
                .orderNo(testQuestion.getOrderNo())
                .parentId(parent != null ? parent.getId() : null)
                .partId(part != null ? part.getId() : null)
                .partNo(partNo)
                .partName(part != null ? part.getName() : "Part " + partNo)
                .section(section.name())
                .title(title)
                .instruction(instruction)
                .passage(passage)
                .audioUrl(audioUrl)
                .imageUrl(imageUrl)
                .isGroupQuestion(parent != null)
                .options(extractOptions(qData))
                .build();
    }

    /**
     * Suy luận số thứ tự Part (1..7) từ tên Part hoặc ID của Part.
     */
    public int resolvePartNumber(Part part) {
        if (part == null) {
            return 1;
        }
        if (part.getName() != null) {
            Matcher matcher = PART_NUMBER_PATTERN.matcher(part.getName());
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // Fallback về ID bên dưới
                }
            }
        }
        return part.getId() != null ? part.getId().intValue() : 1;
    }

    private ESection resolveSection(Question question, Part part, int partNo) {
        if (question.getSections() != null) {
            return question.getSections();
        }
        if (part != null && part.getSections() != null) {
            return part.getSections();
        }
        return partNo <= 4 ? ESection.LISTENING : ESection.READING;
    }

    private String resolveInstruction(JsonNode qData, JsonNode parentData) {
        if (parentData != null) {
            return firstNonBlank(
                    extractText(parentData, "instruction"),
                    extractText(parentData, "text"),
                    extractText(parentData, "context")
            );
        }
        return extractText(qData, "instruction");
    }

    /**
     * Chỉ trả về đoạn văn bài đọc dài cho các phần thi Reading (đặc biệt Part 6, Part 7)
     * nhằm tránh hiển thị lời thoại nghe (Listening) hoặc chia cột thừa trên giao diện.
     */
    private String resolvePassage(int partNo, ESection section, JsonNode qData, JsonNode parentData) {
        if (section != ESection.READING && partNo < PART_READING_PASSAGE_START) {
            return null;
        }
        String parentPassage = parentData != null
                ? firstNonBlank(extractText(parentData, "passage"), extractText(parentData, "content"))
                : null;
        if (parentPassage != null) {
            return parentPassage;
        }
        return firstNonBlank(extractText(qData, "passage"), extractText(qData, "content"));
    }

    private String resolveMediaUrl(
            Question question,
            Question parent,
            JsonNode qData,
            JsonNode parentData,
            EMediaKind targetKind,
            String jsonField
    ) {
        String ownMedia = findMediaUrlInList(question.getQuestionMedias(), targetKind);
        if (ownMedia != null) {
            return ownMedia;
        }
        String ownJsonMedia = extractText(qData, jsonField);
        if (ownJsonMedia != null) {
            return ownJsonMedia;
        }
        if (parent != null) {
            String parentMedia = findMediaUrlInList(parent.getQuestionMedias(), targetKind);
            if (parentMedia != null) {
                return parentMedia;
            }
            return extractText(parentData, jsonField);
        }
        return null;
    }

    private String findMediaUrlInList(List<QuestionMedia> medias, EMediaKind targetKind) {
        if (medias == null || medias.isEmpty()) {
            return null;
        }
        for (QuestionMedia qm : medias) {
            if (qm == null || qm.getMedia() == null) {
                continue;
            }
            if (MediaKindResolver.resolve(qm.getMedia()) == targetKind) {
                String url = qm.getMedia().getSecureUrl();
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        }
        return null;
    }

    private List<DiagnosticOptionResponse> extractOptions(JsonNode qData) {
        if (qData == null || !qData.has("options") || !qData.get("options").isArray()) {
            return Collections.emptyList();
        }
        List<DiagnosticOptionResponse> options = new ArrayList<>();
        for (JsonNode optNode : qData.get("options")) {
            String key = optNode.has("key") ? optNode.get("key").asText("").trim().toUpperCase() : "";
            String text = optNode.has("text") ? optNode.get("text").asText("") : "";
            if (!key.isEmpty()) {
                options.add(DiagnosticOptionResponse.builder()
                        .key(key)
                        .text(text)
                        .build());
            }
        }
        return options;
    }

    private JsonNode parseJsonNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.warn("Không thể parse JSON questionData trong DiagnosticRoomMapper: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText("");
        return value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
