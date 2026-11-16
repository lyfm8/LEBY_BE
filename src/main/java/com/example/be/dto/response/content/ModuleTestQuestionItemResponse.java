package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.question.EQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đại diện một câu hỏi trong danh sách đề thi vượt ải mô-đun (Use Case A07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleTestQuestionItemResponse {

    private Long id;
    private Long questionId;
    private Integer orderNo;
    private Integer points;
    private String questionName;
    private String questionSummary;
    private EQuestionType questionType;
    private Long partId;
    private String partName;
    private ESection section;
    private Integer difficulty;
    private Long parentId;
    private String parentName;
    private List<Long> abilityIds;
    private List<String> abilityNames;
    private boolean isMatchingAbility;
    private boolean hasAttemptHistory;
}
