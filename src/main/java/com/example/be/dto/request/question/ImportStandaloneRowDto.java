package com.example.be.dto.request.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trung gian chứa dữ liệu thô đã phân tích từ một dòng trong sheet "Câu hỏi đơn" của file Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStandaloneRowDto {

    private int rowNumber;
    private String name;
    private String rawPartId;
    private Long partId;
    private String rawAbilityIds;
    private List<Long> abilityIds;
    private String rawDifficulty;
    private Integer difficulty;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctKey;
    private String rawMediaIds;
    private List<Long> mediaIds;
    private String explTranscript;
    private String explTranslation;
    private String explClue;
    private String explAnalysis;
    private String explVocab;
    private String descriptions;
}
