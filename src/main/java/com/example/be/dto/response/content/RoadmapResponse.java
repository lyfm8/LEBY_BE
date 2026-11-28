package com.example.be.dto.response.content;

import com.example.be.enums.content.ELearningPathStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về thông tin toàn cảnh Lộ trình học cá nhân hóa của học viên (UC09).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapResponse {

    private Long learningPathId;

    private Integer version;

    private ELearningPathStatus status;

    private Long targetProfileId;

    private Integer targetScore;

    private String targetDescription;

    private Long diagnosticAttemptId;

    private Integer progressPercent;

    private Integer totalModules;

    private Integer completedModules;

    /**
     * Cờ đánh dấu học viên đã đạt chuẩn 100% cả 7 Part ngay ở bài thi chẩn đoán (UC09 Luồng 4a).
     */
    @JsonProperty("isPassedAllParts")
    private Boolean isPassedAllParts;

    private List<RoadmapModuleItemResponse> modules;

    /**
     * Danh sách mô-đun đã xuất bản được miễn học (ngoài lộ trình cá nhân), sắp xếp theo sequence.
     */
    private List<RoadmapModuleItemResponse> exemptedModules;
}
