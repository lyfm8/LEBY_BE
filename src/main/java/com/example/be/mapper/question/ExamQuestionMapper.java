package com.example.be.mapper.question;

import com.example.be.dto.response.exam.ExamOptionResponse;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.dto.response.question.QuestionVocabularyItemResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaKind;
import com.example.be.service.question.util.MediaKindResolver;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapper lõi dùng chung để trích xuất và chuẩn hóa dữ liệu Câu hỏi TOEIC (ngữ cảnh cha, passage, media,
 * phương án A-B-C-D, đáp án đúng và giải thích chi tiết) cho Diagnostic, Practice (UC12) và Module Test (UC13-UC14).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamQuestionMapper {

    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("Part\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPTION_LABEL_PREFIX_PATTERN = Pattern.compile("^\\([A-Da-d]\\)\\s*");
    private static final int PART_READING_PASSAGE_START = 6;
    private static final int DEFAULT_PART_NO = 1;

    private final ObjectMapper objectMapper;

    /**
     * Dữ liệu nội dung câu hỏi đã chuẩn hóa từ JSON và gộp ngữ cảnh cha (nếu có).
     */
    @Getter
    @Builder
    public static class MappedQuestionContent {
        private Long questionId;
        private Long parentId;
        private Long partId;
        private Integer partNo;
        private String partName;
        private String section;
        private String title;
        private String instruction;
        private String passage;
        private String audioUrl;
        private String imageUrl;
        private Boolean isGroupQuestion;
        private List<ExamOptionResponse> options;
    }

    /**
     * Trích xuất toàn bộ nội dung hiển thị của câu hỏi kèm gộp ngữ cảnh câu hỏi cha và ánh xạ xáo trộn phương án (nếu có).
     *
     * @param question              Thực thể câu hỏi đã nạp sẵn Part, Parent, QuestionMedias
     * @param displayToOriginalMap  Bảng ánh xạ key hiển thị -> key gốc (null nếu giữ nguyên thứ tự gốc)
     * @return {@link MappedQuestionContent}
     */
    public MappedQuestionContent extractQuestionContent(
            Question question,
            Map<String, String> displayToOriginalMap
    ) {
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

        List<ExamOptionResponse> originalOptions = extractRawOptions(qData);
        List<ExamOptionResponse> finalOptions = applyOptionShuffleMap(originalOptions, displayToOriginalMap);

        return MappedQuestionContent.builder()
                .questionId(question.getId())
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
                .options(finalOptions)
                .build();
    }

    /**
     * Sinh bảng ánh xạ xáo trộn ngẫu nhiên các phương án A-B-C-D cho một câu hỏi (BR05).
     * Key là nhãn hiển thị mới trên giao diện, Value là nhãn gốc trong ngân hàng câu hỏi.
     */
    public Map<String, String> buildShuffleOptionMap(Question question) {
        JsonNode qData = parseJsonNode(question.getQuestionData());
        List<ExamOptionResponse> rawOptions = extractRawOptions(qData);
        if (rawOptions.size() <= 1) {
            return Collections.emptyMap();
        }

        List<String> displayKeys = rawOptions.stream()
                .map(ExamOptionResponse::getKey)
                .toList();
        List<String> shuffledOriginalKeys = new ArrayList<>(displayKeys);
        Collections.shuffle(shuffledOriginalKeys);

        Map<String, String> optionMap = new LinkedHashMap<>();
        for (int i = 0; i < displayKeys.size(); i++) {
            optionMap.put(displayKeys.get(i), shuffledOriginalKeys.get(i));
        }
        return optionMap;
    }

    /**
     * Quy đổi ký hiệu phương án đang hiển thị trên giao diện về ký hiệu gốc trong DB (khi có shuffle).
     */
    public String resolveOriginalKeyFromDisplay(String displayKey, Map<String, String> displayToOriginalMap) {
        String normalized = normalizeOptionKey(displayKey);
        if (normalized == null) {
            return null;
        }
        if (displayToOriginalMap == null || displayToOriginalMap.isEmpty()) {
            return normalized;
        }
        return normalizeOptionKey(displayToOriginalMap.getOrDefault(normalized, normalized));
    }

    /**
     * Quy đổi ký hiệu phương án gốc trong DB sang ký hiệu hiển thị trên phòng thi của lượt đó (khi có shuffle).
     */
    public String resolveDisplayKeyFromOriginal(String originalKey, Map<String, String> displayToOriginalMap) {
        String normalized = normalizeOptionKey(originalKey);
        if (normalized == null || displayToOriginalMap == null || displayToOriginalMap.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : displayToOriginalMap.entrySet()) {
            if (normalized.equalsIgnoreCase(entry.getValue())) {
                return normalizeOptionKey(entry.getKey());
            }
        }
        return normalized;
    }

    /**
     * Trích xuất ký hiệu đáp án (A/B/C/D) từ chuỗi JSON hoặc ký tự đơn.
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
            return normalizeOptionKey(trimmed);
        }
        JsonNode node = parseJsonNode(trimmed);
        if (node == null) {
            return null;
        }
        if (node.has("key") && !node.get("key").isNull()) {
            String val = cleanBlankToNull(node.get("key").asText(""));
            if (val != null) {
                return normalizeOptionKey(val);
            }
        }
        if (node.has("selected") && !node.get("selected").isNull()) {
            String val = cleanBlankToNull(node.get("selected").asText(""));
            if (val != null) {
                return normalizeOptionKey(val);
            }
        }
        return null;
    }

    /**
     * Hợp nhất giải thích chi tiết của câu con với transcript/translation từ câu cha (nếu câu con thiếu).
     */
    public QuestionExplanationResponse resolveExplanation(Question question) {
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

    /**
     * Suy luận số thứ tự Part (1..7) từ tên Part hoặc ID của Part.
     */
    public int resolvePartNumber(Part part) {
        if (part == null) {
            return DEFAULT_PART_NO;
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
        return part.getId() != null ? part.getId().intValue() : DEFAULT_PART_NO;
    }

    /**
     * Xác định nhóm kỹ năng LISTENING hoặc READING cho câu hỏi.
     */
    public ESection resolveSection(Question question, Part part, int partNo) {
        if (question.getSections() != null) {
            return question.getSections();
        }
        if (part != null && part.getSections() != null) {
            return part.getSections();
        }
        return partNo <= 4 ? ESection.LISTENING : ESection.READING;
    }

    /**
     * Chuẩn hóa ký hiệu phương án về chữ in hoa (A/B/C/D).
     */
    public String normalizeOptionKey(String key) {
        String cleaned = cleanBlankToNull(key);
        return cleaned != null ? cleaned.toUpperCase(Locale.ROOT) : null;
    }

    private List<ExamOptionResponse> applyOptionShuffleMap(
            List<ExamOptionResponse> originalOptions,
            Map<String, String> displayToOriginalMap
    ) {
        if (displayToOriginalMap == null || displayToOriginalMap.isEmpty() || originalOptions.isEmpty()) {
            return originalOptions;
        }

        Map<String, ExamOptionResponse> optionByOriginalKey = new LinkedHashMap<>();
        for (ExamOptionResponse opt : originalOptions) {
            optionByOriginalKey.put(normalizeOptionKey(opt.getKey()), opt);
        }

        List<ExamOptionResponse> remapped = new ArrayList<>(originalOptions.size());
        List<Map.Entry<String, String>> orderedEntries = new ArrayList<>(displayToOriginalMap.entrySet());
        orderedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, String> entry : orderedEntries) {
            String displayKey = normalizeOptionKey(entry.getKey());
            String originalKey = normalizeOptionKey(entry.getValue());
            ExamOptionResponse originalOpt = optionByOriginalKey.get(originalKey);
            if (originalOpt != null && displayKey != null) {
                // Gỡ bỏ tiền tố "(A) " / "(B) " cũ khi xáo trộn để tránh lệch ký hiệu hiển thị trên badge
                String cleanedText = stripOptionPrefix(originalOpt.getText());
                remapped.add(ExamOptionResponse.builder()
                        .key(displayKey)
                        .text(cleanedText)
                        .build());
            }
        }
        return remapped.isEmpty() ? originalOptions : remapped;
    }

    private String stripOptionPrefix(String rawText) {
        if (rawText == null) {
            return "";
        }
        return OPTION_LABEL_PREFIX_PATTERN.matcher(rawText.trim()).replaceFirst("").trim();
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
     * Chỉ trả về đoạn văn bài đọc dài cho phần Reading (hoặc Part 6, 7) để tránh lộ transcript câu Listening.
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

    private List<ExamOptionResponse> extractRawOptions(JsonNode qData) {
        if (qData == null || !qData.has("options") || !qData.get("options").isArray()) {
            return Collections.emptyList();
        }
        List<ExamOptionResponse> options = new ArrayList<>();
        for (JsonNode optNode : qData.get("options")) {
            String key = optNode.has("key") ? optNode.get("key").asText("").trim().toUpperCase(Locale.ROOT) : "";
            String text = optNode.has("text") ? optNode.get("text").asText("") : "";
            if (!key.isEmpty()) {
                options.add(ExamOptionResponse.builder()
                        .key(key)
                        .text(text)
                        .build());
            }
        }
        return options;
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
            log.warn("Không thể parse JSON câu hỏi trong ExamQuestionMapper: {}", e.getMessage());
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

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
