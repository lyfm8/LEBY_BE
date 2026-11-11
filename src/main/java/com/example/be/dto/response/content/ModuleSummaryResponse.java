package com.example.be.dto.response.content;

import com.example.be.enums.content.EModuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tóm tắt Module (kèm danh sách bài học con) phục vụ hiển thị cây danh mục 25% bên trái.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleSummaryResponse {

    private Long id;
    private String title;
    private String type;
    private Integer sequence;
    private EModuleStatus status;
    private Integer defaultPassScore;
    private Integer requiredCompletionRate;
    private Long partId;
    private String partName;
    private List<Long> abilityIds;
    private List<String> abilityNames;
    private int totalLessons;
    private int totalVideoLessons;
    private int totalPracticeLessons;
    private List<LessonSummaryResponse> lessons;
}

