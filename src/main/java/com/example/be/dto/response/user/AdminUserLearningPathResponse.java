package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tiến độ lộ trình học tập hiện tại của học viên dành cho Quản trị viên (UC A02 - Bước 6 & Luồng 7b).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserLearningPathResponse {
    private Long learningPathId;
    private Integer version;
    private String status;
    private Integer targetScore;
    private Integer progressPercent;
    private Integer totalModules;
    private Integer completedModules;
    private LocalDateTime createdAt;
    private List<AdminUserLearningPathItemDto> items;
}
