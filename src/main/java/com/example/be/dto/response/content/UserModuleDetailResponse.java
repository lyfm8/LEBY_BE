package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về thông tin chi tiết một Module kèm danh sách bài học và điều kiện bài kiểm tra vượt ải (UC10).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModuleDetailResponse {

    private Long moduleId;

    private String title;

    private String partName;

    private ESection section;

    private String description;

    private String abilityName;

    private ELessonStatus status;

    private String statusLabel;

    private Boolean inRoadmap;

    private ECatalogAccessMode accessMode;

    private Integer totalLessons;

    private Integer completedLessons;

    private Integer totalVideoLessons;

    private Integer completedVideoLessons;

    private Integer progressPercent;

    private Integer requiredCompletionRate;

    private List<UserModuleLessonResponse> lessons;

    private UserModuleTestInfoResponse moduleTest;
}
