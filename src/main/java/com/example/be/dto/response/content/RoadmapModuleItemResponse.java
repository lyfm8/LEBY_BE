package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một chặng Module trên dòng thời gian của Lộ trình học cá nhân (UC09).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapModuleItemResponse {

    /**
     * ID của Module để FE điều hướng thẳng sang /modules/{id}.
     */
    private Long id;

    private Long learningPathItemId;

    private Integer orderNo;

    private Integer sequence;

    private String title;

    private String description;

    private Long partId;

    private String partName;

    private ESection section;

    private Long abilityId;

    private String abilityName;

    private String reason;

    private Integer totalLessons;

    private Integer completedLessons;

    private Integer progressPercent;

    private ELessonStatus status;

    private String statusLabel;

    /**
     * True nếu mô-đun nằm trong lộ trình cá nhân; false nếu là mô-đun đã vượt qua được miễn học.
     */
    private Boolean inRoadmap;
}
