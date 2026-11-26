package com.example.be.dto.request.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trung gian chứa thông tin một dòng câu hỏi con (CÂU CON) thuộc một chùm trong sheet "Chùm câu hỏi".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportGroupItemRowDto {

    private int rowNumber;
    private String groupRowId;
    private String name;
    private String rawOrderNo;
    private Integer orderNo;
    private String text;
    private String rawAbilityIds;
    private List<Long> abilityIds;
    private String rawDifficulty;
    private Integer difficulty;
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
}
