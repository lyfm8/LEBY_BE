package com.example.be.dto.response.content;

import com.example.be.enums.content.ELearningPathStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp toàn bộ Thư viện khóa học 7 Part TOEIC dành cho học viên (UC17 Catalog).
 * Chứa ngữ cảnh lộ trình hiện hành của học viên, thống kê tổng quan và danh sách nhóm theo 7 Part.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCatalogResponse {

    private Boolean hasLearningPath;

    private ELearningPathStatus learningPathStatus;

    private Integer targetScore;

    private String targetDescription;

    private Integer totalParts;

    private Integer totalModules;

    private Integer totalLessons;

    private Integer roadmapModuleCount;

    private Integer completedRoadmapModuleCount;

    private Integer freeStudyModuleCount;

    private List<CatalogPartGroupResponse> parts;
}
