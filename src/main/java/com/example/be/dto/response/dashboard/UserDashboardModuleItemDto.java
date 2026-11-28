package com.example.be.dto.response.dashboard;

import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.ability.ESection;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tóm tắt một chặng mô-đun sắp tới trên lộ trình hiển thị ở cột phải Dashboard (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardModuleItemDto {

    private Long moduleId;
    private Long learningPathItemId;
    private Integer orderNo;
    private Integer sequence;
    private String title;
    private String description;
    private String partName;
    private ESection section;
    private ELessonStatus status;
    private String statusLabel;
    private int progressPercent;

    @JsonProperty("isLocked")
    private Boolean isLocked;

    @JsonProperty("isUnderMaintenance")
    private Boolean isUnderMaintenance;
}
