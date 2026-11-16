package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO chi tiết cấu hình Bài thi vượt ải của một Mô-đun (Use Case A07).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleTestConfigDetailResponse {

    private Long moduleId;
    private String moduleTitle;
    private Integer sequence;
    private EModuleStatus moduleStatus;
    private Long partId;
    private String partName;
    private ESection section;
    private List<Long> abilityIds;
    private List<String> abilityNames;
    private int totalLessons;
    private int totalVideoLessons;
    private Integer defaultPassScore;
    private Integer requiredCompletionRate;
    private Integer durationMinutes;
    private Integer questionCount;
    private int currentQuestionCount;
    private Boolean shuffleQuestions;
    private EModuleTestSelectionMode selectionMode;
    private long availableAbilityQuestionCount;
    private boolean hasTestConfigured;
    private boolean hasNoLessonsWarning;
    private List<ModuleTestQuestionItemResponse> questions;
}
