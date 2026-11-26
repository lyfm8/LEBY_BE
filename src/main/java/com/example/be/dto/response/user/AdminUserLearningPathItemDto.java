package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO một chặng mô-đun trong lộ trình học của học viên khi Quản trị viên tra cứu (UC A02 - Luồng 7b).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserLearningPathItemDto {
    private Long id;
    private Integer orderNo;
    private Long moduleId;
    private String moduleTitle;
    private String moduleDescription;
    private String partName;
    private String section;
    private String abilityName;
    private String reason;
    private String status;
    private String statusLabel;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer progressPercent;
    private Boolean isCorrect;
}
