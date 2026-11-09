package com.example.be.mapper.question;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.QuestionExplanationRequest;
import com.example.be.dto.request.question.QuestionGroupItemRequest;
import com.example.be.dto.request.question.QuestionOptionRequest;
import com.example.be.dto.request.question.UpdateQuestionGroupRequest;
import com.example.be.dto.request.question.UpdateStandaloneQuestionRequest;
import com.example.be.dto.response.question.QuestionAbilityResponse;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.dto.response.question.QuestionGroupItemResponse;
import com.example.be.dto.response.question.QuestionListItemResponse;
import com.example.be.dto.response.question.QuestionMediaResponse;
import com.example.be.dto.response.question.QuestionOptionResponse;
import com.example.be.dto.response.question.QuestionVocabularyItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.media.EMediaKind;
import com.example.be.enums.question.EQuestionType;
import com.example.be.service.question.util.MediaKindResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyển đổi giữa Entity Question và các đối tượng DTO liên quan.
 * Tuân thủ quy chuẩn không dùng FQN trong thân code và sử dụng ObjectMapper của Spring Boot 4.
 * Lưu trữ JSON dưới dạng String an toàn trong JPA Entity để tránh xung đột deserialization của Hibernate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionMapper {

    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi dữ liệu tạo mới câu hỏi đơn lập sang Question Entity.
     */
    public Question toStandaloneEntity(
            CreateStandaloneQuestionRequest request,
            Part part,
            List<Ability> abilities,
            List<Media> medias
    ) {
        Question question = new Question();
        question.setType(EQuestionType.SINGLE_CHOICE);
        question.setName(request.getName().trim());
        question.setPart(part);
        question.setSections(part.getSections());
        question.setDifficulty(request.getDifficulty());
        question.setDescriptions(request.getDescriptions());

        question.setQuestionData(buildStandaloneQuestionData(request.getText(), request.getOptions()));
        question.setCorrectAnswer(buildCorrectAnswerNode(request.getCorrectKey()));
        question.setExplanation(buildExplanationNode(request.getExplanation()));

        attachAbilities(question, abilities);
        attachMedias(question, medias);

        return question;
    }

    /**
     * Cập nhật thông tin cho câu hỏi đơn lập hiện có.
     */
    public void updateStandaloneEntity(
            Question question,
            UpdateStandaloneQuestionRequest request,
            Part part,
            List<Ability> abilities,
            List<Media> medias
    ) {
        question.setName(request.getName().trim());
        question.setPart(part);
        question.setSections(part.getSections());
        question.setDifficulty(request.getDifficulty());
        question.setDescriptions(request.getDescriptions());

        question.setQuestionData(buildStandaloneQuestionData(request.getText(), request.getOptions()));
        question.setCorrectAnswer(buildCorrectAnswerNode(request.getCorrectKey()));
        question.setExplanation(buildExplanationNode(request.getExplanation()));

        attachAbilities(question, abilities);
        attachMedias(question, medias);
    }

    /**
     * Tạo Entity chùm câu hỏi cha (Ngữ cảnh / Bài đọc / Audio chung).
     */
    public Question toGroupParentEntity(
            CreateQuestionGroupRequest request,
            Part part,
            List<Media> medias
    ) {
        Question question = new Question();
        question.setType(EQuestionType.SINGLE_CHOICE);
        question.setName(request.getName().trim());
        question.setPart(part);
        question.setSections(part.getSections());
        question.setDescriptions(request.getDescriptions());

        question.setQuestionData(buildGroupQuestionData(request.getInstruction(), request.getPassage()));
        question.setExplanation(buildGroupExplanationNode(request.getTranscript(), request.getTranslation()));

        attachMedias(question, medias);
        return question;
    }

    /**
     * Cập nhật thông tin Entity chùm câu hỏi cha.
     */
    public void updateGroupParentEntity(
            Question question,
            UpdateQuestionGroupRequest request,
            Part part,
            List<Media> medias
    ) {
        question.setName(request.getName().trim());
        question.setPart(part);
        question.setSections(part.getSections());
        question.setDescriptions(request.getDescriptions());

        question.setQuestionData(buildGroupQuestionData(request.getInstruction(), request.getPassage()));
        question.setExplanation(buildGroupExplanationNode(request.getTranscript(), request.getTranslation()));

        attachMedias(question, medias);
    }

    /**
     * Tạo hoặc cập nhật câu hỏi con trong chùm.
     */
    public Question buildGroupItemEntity(
            QuestionGroupItemRequest itemRequest,
            Question parent,
            List<Ability> abilities,
            List<Media> medias,
            int orderNo,
            Question existingItem
    ) {
        Question question = existingItem != null ? existingItem : new Question();
        question.setParent(parent);
        question.setOrderNo(orderNo);
        question.setType(EQuestionType.SINGLE_CHOICE);
        question.setName(itemRequest.getName().trim());
        question.setPart(parent.getPart());
        question.setSections(parent.getSections());
        question.setDifficulty(itemRequest.getDifficulty());

        question.setQuestionData(buildStandaloneQuestionData(itemRequest.getText(), itemRequest.getOptions()));
        question.setCorrectAnswer(buildCorrectAnswerNode(itemRequest.getCorrectKey()));
        question.setExplanation(buildExplanationNode(itemRequest.getExplanation()));

        attachAbilities(question, abilities);
        attachMedias(question, medias);

        return question;
    }

    /**
     * Chuyển đổi Entity sang QuestionListItemResponse.
     */
    public QuestionListItemResponse toListItemResponse(Question question, int subQuestionCount) {
        boolean isGroup = subQuestionCount > 0
                || (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty());

        List<String> abilityNames = question.getQuestionAbilities() != null
                ? question.getQuestionAbilities().stream()
                        .map(qa -> qa.getAbility().getName())
                        .distinct()
                        .toList()
                : Collections.emptyList();

        List<EMediaKind> mediaKinds = question.getQuestionMedias() != null
                ? question.getQuestionMedias().stream()
                        .map(qm -> MediaKindResolver.resolve(qm.getMedia()))
                        .distinct()
                        .toList()
                : Collections.emptyList();

        String summary = extractSummary(question);

        return QuestionListItemResponse.builder()
                .id(question.getId())
                .name(question.getName())
                .summary(summary)
                .type(question.getType())
                .partId(question.getPart() != null ? question.getPart().getId() : null)
                .partName(question.getPart() != null ? question.getPart().getName() : null)
                .section(question.getSections())
                .difficulty(question.getDifficulty())
                .isGroup(isGroup)
                .itemCount(subQuestionCount)
                .abilityNames(abilityNames)
                .mediaKinds(mediaKinds)
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi chi tiết Question Entity sang QuestionDetailResponse.
     */
    public QuestionDetailResponse toDetailResponse(Question question) {
        boolean isGroup = question.getSubQuestions() != null && !question.getSubQuestions().isEmpty();

        String instruction = null;
        String passage = null;
        String text = null;
        List<QuestionOptionResponse> options = new ArrayList<>();

        JsonNode qData = parseJsonNode(question.getQuestionData());
        if (qData != null) {
            if (qData.has("instruction")) {
                instruction = qData.get("instruction").asText(null);
            }
            if (qData.has("passage")) {
                passage = qData.get("passage").asText(null);
            }
            if (qData.has("text")) {
                text = qData.get("text").asText(null);
            }
            if (qData.has("options") && qData.get("options").isArray()) {
                for (JsonNode optNode : qData.get("options")) {
                    options.add(QuestionOptionResponse.builder()
                            .key(optNode.has("key") ? optNode.get("key").asText() : "")
                            .text(optNode.has("text") ? optNode.get("text").asText() : "")
                            .build());
                }
            }
        }

        String correctKey = null;
        JsonNode cNode = parseJsonNode(question.getCorrectAnswer());
        if (cNode != null) {
            if (cNode.has("key")) {
                correctKey = cNode.get("key").asText(null);
            } else if (cNode.has("selected")) {
                correctKey = cNode.get("selected").asText(null);
            }
        }

        QuestionExplanationResponse explanationResponse = parseExplanationNode(parseJsonNode(question.getExplanation()));
        List<QuestionMediaResponse> mediaResponses = toMediaResponses(question.getQuestionMedias());
        List<QuestionAbilityResponse> abilityResponses = toAbilityResponses(question.getQuestionAbilities());

        List<QuestionGroupItemResponse> items = new ArrayList<>();
        if (isGroup) {
            for (Question sub : question.getSubQuestions()) {
                items.add(toGroupItemResponse(sub));
            }
        }

        return QuestionDetailResponse.builder()
                .id(question.getId())
                .name(question.getName())
                .type(question.getType())
                .isGroup(isGroup)
                .partId(question.getPart() != null ? question.getPart().getId() : null)
                .partName(question.getPart() != null ? question.getPart().getName() : null)
                .section(question.getSections())
                .difficulty(question.getDifficulty())
                .descriptions(question.getDescriptions())
                .instruction(instruction)
                .passage(passage)
                .text(text)
                .options(options)
                .correctKey(correctKey)
                .explanation(explanationResponse)
                .medias(mediaResponses)
                .abilities(abilityResponses)
                .items(items)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi một câu hỏi con trong chùm sang QuestionGroupItemResponse.
     */
    public QuestionGroupItemResponse toGroupItemResponse(Question sub) {
        String itemText = null;
        List<QuestionOptionResponse> options = new ArrayList<>();

        JsonNode qData = parseJsonNode(sub.getQuestionData());
        if (qData != null) {
            if (qData.has("text")) {
                itemText = qData.get("text").asText(null);
            }
            if (qData.has("options") && qData.get("options").isArray()) {
                for (JsonNode optNode : qData.get("options")) {
                    options.add(QuestionOptionResponse.builder()
                            .key(optNode.has("key") ? optNode.get("key").asText() : "")
                            .text(optNode.has("text") ? optNode.get("text").asText() : "")
                            .build());
                }
            }
        }

        String correctKey = null;
        JsonNode cNode = parseJsonNode(sub.getCorrectAnswer());
        if (cNode != null) {
            if (cNode.has("key")) {
                correctKey = cNode.get("key").asText(null);
            } else if (cNode.has("selected")) {
                correctKey = cNode.get("selected").asText(null);
            }
        }

        return QuestionGroupItemResponse.builder()
                .id(sub.getId())
                .name(sub.getName())
                .orderNo(sub.getOrderNo())
                .text(itemText)
                .options(options)
                .correctKey(correctKey)
                .difficulty(sub.getDifficulty())
                .explanation(parseExplanationNode(parseJsonNode(sub.getExplanation())))
                .medias(toMediaResponses(sub.getQuestionMedias()))
                .abilities(toAbilityResponses(sub.getQuestionAbilities()))
                .build();
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private String buildStandaloneQuestionData(String text, List<QuestionOptionRequest> options) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", text != null ? text.trim() : "");

        ArrayNode optionsArray = node.putArray("options");
        if (options != null) {
            for (QuestionOptionRequest opt : options) {
                ObjectNode optNode = optionsArray.addObject();
                optNode.put("key", opt.getKey() != null ? opt.getKey().trim().toUpperCase() : "");
                optNode.put("text", opt.getText() != null ? opt.getText().trim() : "");
            }
        }
        return node.toString();
    }

    private String buildCorrectAnswerNode(String correctKey) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("key", correctKey != null ? correctKey.trim().toUpperCase() : "");
        return node.toString();
    }

    private String buildExplanationNode(QuestionExplanationRequest request) {
        if (request == null) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("transcript", request.getTranscript() != null ? request.getTranscript() : "");
        node.put("translation", request.getTranslation() != null ? request.getTranslation() : "");
        node.put("clue", request.getClue() != null ? request.getClue() : "");
        node.put("analysis", request.getAnalysis() != null ? request.getAnalysis() : "");

        ArrayNode vocabArray = node.putArray("vocabulary");
        if (request.getVocabulary() != null) {
            for (var v : request.getVocabulary()) {
                ObjectNode vNode = vocabArray.addObject();
                vNode.put("word", v.getWord() != null ? v.getWord() : "");
                vNode.put("type", v.getType() != null ? v.getType() : "");
                vNode.put("meaning", v.getMeaning() != null ? v.getMeaning() : "");
            }
        }
        return node.toString();
    }

    private String buildGroupQuestionData(String instruction, String passage) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("instruction", instruction != null ? instruction.trim() : "");
        node.put("passage", passage != null ? passage.trim() : "");
        return node.toString();
    }

    private String buildGroupExplanationNode(String transcript, String translation) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("transcript", transcript != null ? transcript : "");
        node.put("translation", translation != null ? translation : "");
        return node.toString();
    }

    private JsonNode parseJsonNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.warn("Không thể parse dữ liệu JSON từ chuỗi: {}", e.getMessage());
            return null;
        }
    }

    private QuestionExplanationResponse parseExplanationNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<QuestionVocabularyItemResponse> vocabulary = new ArrayList<>();
        if (node.has("vocabulary") && node.get("vocabulary").isArray()) {
            for (JsonNode vNode : node.get("vocabulary")) {
                vocabulary.add(QuestionVocabularyItemResponse.builder()
                        .word(vNode.has("word") ? vNode.get("word").asText("") : "")
                        .type(vNode.has("type") ? vNode.get("type").asText("") : "")
                        .meaning(vNode.has("meaning") ? vNode.get("meaning").asText("") : "")
                        .build());
            }
        }

        return QuestionExplanationResponse.builder()
                .transcript(node.has("transcript") ? node.get("transcript").asText("") : "")
                .translation(node.has("translation") ? node.get("translation").asText("") : "")
                .clue(node.has("clue") ? node.get("clue").asText("") : "")
                .analysis(node.has("analysis") ? node.get("analysis").asText("") : "")
                .vocabulary(vocabulary)
                .build();
    }

    private List<QuestionMediaResponse> toMediaResponses(List<QuestionMedia> questionMedias) {
        if (questionMedias == null || questionMedias.isEmpty()) {
            return Collections.emptyList();
        }
        return questionMedias.stream().map(qm -> {
            Media m = qm.getMedia();
            return QuestionMediaResponse.builder()
                    .mediaId(m.getId())
                    .publicId(m.getPublicId())
                    .url(m.getSecureUrl())
                    .kind(MediaKindResolver.resolve(m))
                    .format(m.getFormat())
                    .bytes(m.getBytes())
                    .originalFilename(m.getOriginalFilename())
                    .duration(m.getDuration())
                    .width(m.getWidth())
                    .height(m.getHeight())
                    .orderNo(qm.getOrderNo())
                    .build();
        }).toList();
    }

    private List<QuestionAbilityResponse> toAbilityResponses(List<QuestionAbility> questionAbilities) {
        if (questionAbilities == null || questionAbilities.isEmpty()) {
            return Collections.emptyList();
        }
        return questionAbilities.stream().map(qa -> QuestionAbilityResponse.builder()
                .abilityId(qa.getAbility().getId())
                .abilityName(qa.getAbility().getName())
                .orderNo(qa.getOrderNo())
                .points(qa.getPoints())
                .build()
        ).toList();
    }

    private void attachAbilities(Question question, List<Ability> abilities) {
        question.getQuestionAbilities().clear();
        if (abilities != null) {
            int order = 1;
            for (Ability ability : abilities) {
                QuestionAbility qa = new QuestionAbility();
                qa.setQuestion(question);
                qa.setAbility(ability);
                qa.setOrderNo(order++);
                qa.setPoints(1);
                question.getQuestionAbilities().add(qa);
            }
        }
    }

    private void attachMedias(Question question, List<Media> medias) {
        question.getQuestionMedias().clear();
        if (medias != null) {
            int order = 1;
            for (Media media : medias) {
                QuestionMedia qm = QuestionMedia.builder()
                        .question(question)
                        .media(media)
                        .orderNo(order++)
                        .build();
                question.getQuestionMedias().add(qm);
            }
        }
    }

    private String extractSummary(Question question) {
        JsonNode qData = parseJsonNode(question.getQuestionData());
        if (qData != null) {
            if (qData.has("text") && !qData.get("text").asText().isBlank()) {
                return truncate(qData.get("text").asText(), 120);
            }
            if (qData.has("passage") && !qData.get("passage").asText().isBlank()) {
                return truncate(qData.get("passage").asText(), 120);
            }
            if (qData.has("instruction") && !qData.get("instruction").asText().isBlank()) {
                return truncate(qData.get("instruction").asText(), 120);
            }
        }
        if (question.getDescriptions() != null && !question.getDescriptions().isBlank()) {
            return truncate(question.getDescriptions(), 120);
        }
        return question.getName();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String clean = text.replace("\n", " ").trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength) + "...";
    }
}
