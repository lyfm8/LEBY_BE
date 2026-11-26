package com.example.be.mapper.question;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.ImportGroupItemRowDto;
import com.example.be.dto.request.question.ImportGroupParentRowDto;
import com.example.be.dto.request.question.ImportStandaloneRowDto;
import com.example.be.dto.request.question.QuestionExplanationRequest;
import com.example.be.dto.request.question.QuestionGroupItemRequest;
import com.example.be.dto.request.question.QuestionMediaItemRequest;
import com.example.be.dto.request.question.QuestionOptionRequest;
import com.example.be.dto.request.question.QuestionVocabularyItemRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Mapper chuyên biệt chuyển đổi từ DTO dòng Excel trung gian sang Request DTO tạo câu hỏi chuẩn của hệ thống.
 */
@Component
public class QuestionImportMapper {

    /**
     * Chuyển đổi một dòng câu hỏi đơn đã kiểm tra hợp lệ sang {@link CreateStandaloneQuestionRequest}.
     *
     * @param row DTO dòng câu hỏi đơn
     * @return CreateStandaloneQuestionRequest
     */
    public CreateStandaloneQuestionRequest toStandaloneRequest(ImportStandaloneRowDto row) {
        return CreateStandaloneQuestionRequest.builder()
                .name(trimToEmpty(row.getName()))
                .partId(row.getPartId())
                .abilityIds(row.getAbilityIds())
                .difficulty(row.getDifficulty())
                .text(trimToNull(row.getText()))
                .options(buildOptions(row.getOptionA(), row.getOptionB(), row.getOptionC(), row.getOptionD()))
                .correctKey(normalizeKey(row.getCorrectKey()))
                .explanation(buildExplanation(
                        row.getExplTranscript(),
                        row.getExplTranslation(),
                        row.getExplClue(),
                        row.getExplAnalysis(),
                        row.getExplVocab()
                ))
                .medias(buildMediaItems(row.getMediaIds()))
                .descriptions(trimToNull(row.getDescriptions()))
                .build();
    }

    /**
     * Chuyển đổi một chùm câu hỏi (gồm 1 dòng CHÙM và danh sách dòng CÂU CON) sang {@link CreateQuestionGroupRequest}.
     *
     * @param parentRow DTO dòng CHÙM cha
     * @param itemRows  Danh sách DTO dòng CÂU CON
     * @return CreateQuestionGroupRequest
     */
    public CreateQuestionGroupRequest toGroupRequest(
            ImportGroupParentRowDto parentRow,
            List<ImportGroupItemRowDto> itemRows
    ) {
        List<QuestionGroupItemRequest> items = new ArrayList<>();
        int fallbackOrder = 1;
        for (ImportGroupItemRowDto itemRow : itemRows) {
            items.add(toGroupItemRequest(itemRow, fallbackOrder++));
        }

        return CreateQuestionGroupRequest.builder()
                .name(trimToEmpty(parentRow.getName()))
                .partId(parentRow.getPartId())
                .instruction(trimToNull(parentRow.getInstruction()))
                .passage(trimToNull(parentRow.getPassage()))
                .transcript(trimToNull(parentRow.getTranscript()))
                .translation(trimToNull(parentRow.getTranslation()))
                .medias(buildMediaItems(parentRow.getMediaIds()))
                .items(items)
                .descriptions(trimToNull(parentRow.getDescriptions()))
                .build();
    }

    /**
     * Chuyển đổi một dòng CÂU CON trong chùm sang {@link QuestionGroupItemRequest}.
     *
     * @param itemRow       DTO dòng câu con
     * @param fallbackOrder Thứ tự mặc định nếu cột thứ tự không được điền
     * @return QuestionGroupItemRequest
     */
    public QuestionGroupItemRequest toGroupItemRequest(ImportGroupItemRowDto itemRow, int fallbackOrder) {
        int resolvedOrder = itemRow.getOrderNo() != null && itemRow.getOrderNo() > 0
                ? itemRow.getOrderNo()
                : fallbackOrder;

        return QuestionGroupItemRequest.builder()
                .name(trimToEmpty(itemRow.getName()))
                .orderNo(resolvedOrder)
                .text(trimToNull(itemRow.getText()))
                .options(buildOptions(itemRow.getOptionA(), itemRow.getOptionB(), itemRow.getOptionC(), itemRow.getOptionD()))
                .correctKey(normalizeKey(itemRow.getCorrectKey()))
                .abilityIds(itemRow.getAbilityIds())
                .difficulty(itemRow.getDifficulty())
                .explanation(buildExplanation(
                        itemRow.getExplTranscript(),
                        itemRow.getExplTranslation(),
                        itemRow.getExplClue(),
                        itemRow.getExplAnalysis(),
                        itemRow.getExplVocab()
                ))
                .medias(buildMediaItems(itemRow.getMediaIds()))
                .build();
    }

    private List<QuestionOptionRequest> buildOptions(
            String optionA,
            String optionB,
            String optionC,
            String optionD
    ) {
        List<QuestionOptionRequest> options = new ArrayList<>();
        options.add(new QuestionOptionRequest("A", trimToEmpty(optionA)));
        options.add(new QuestionOptionRequest("B", trimToEmpty(optionB)));
        options.add(new QuestionOptionRequest("C", trimToEmpty(optionC)));
        if (optionD != null && !optionD.isBlank()) {
            options.add(new QuestionOptionRequest("D", optionD.trim()));
        }
        return options;
    }

    private List<QuestionMediaItemRequest> buildMediaItems(List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<QuestionMediaItemRequest> result = new ArrayList<>(mediaIds.size());
        int order = 1;
        for (Long mediaId : mediaIds) {
            result.add(QuestionMediaItemRequest.builder()
                    .mediaId(mediaId)
                    .orderNo(order++)
                    .build());
        }
        return result;
    }

    private QuestionExplanationRequest buildExplanation(
            String transcript,
            String translation,
            String clue,
            String analysis,
            String rawVocab
    ) {
        List<QuestionVocabularyItemRequest> vocabulary = parseVocabulary(rawVocab);
        boolean hasContent = isNotBlank(transcript)
                || isNotBlank(translation)
                || isNotBlank(clue)
                || isNotBlank(analysis)
                || !vocabulary.isEmpty();

        if (!hasContent) {
            return null;
        }

        return QuestionExplanationRequest.builder()
                .transcript(trimToEmpty(transcript))
                .translation(trimToEmpty(translation))
                .clue(trimToEmpty(clue))
                .analysis(trimToEmpty(analysis))
                .vocabulary(vocabulary)
                .build();
    }

    private List<QuestionVocabularyItemRequest> parseVocabulary(String rawVocab) {
        if (rawVocab == null || rawVocab.isBlank()) {
            return Collections.emptyList();
        }
        List<QuestionVocabularyItemRequest> items = new ArrayList<>();
        String[] entries = rawVocab.split(";");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":", 3);
            if (parts.length == 3) {
                items.add(QuestionVocabularyItemRequest.builder()
                        .word(parts[0].trim())
                        .type(parts[1].trim().toLowerCase(Locale.ROOT))
                        .meaning(parts[2].trim())
                        .build());
            }
        }
        return items;
    }

    private String normalizeKey(String key) {
        return key != null ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String trimToEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
