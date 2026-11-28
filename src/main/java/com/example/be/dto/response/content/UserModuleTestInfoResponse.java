package com.example.be.dto.response.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO mô tả thông tin cấu hình động và trạng thái mở khóa của Bài kiểm tra vượt ải trong trang Chi tiết Module (UC10).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModuleTestInfoResponse {

    private Long id;

    private String title;

    private String description;

    private Integer passScore;

    private Integer targetScore;

    @JsonProperty("isUnlocked")
    private Boolean isUnlocked;

    private Integer totalQuestions;

    private Integer durationMinutes;

    @JsonProperty("hasPassed")
    private Boolean hasPassed;

    /**
     * Cờ báo hiệu học viên đã bảo lưu toàn bộ bài giảng từ lộ trình trước nhưng cần thi lại vượt ải
     * theo ngưỡng điểm đỗ của mục tiêu AIM mới cao hơn (BR06 bản cập nhật 25/11).
     */
    @JsonProperty("needsRetakeForNewAim")
    private Boolean needsRetakeForNewAim;

    private Integer bestScore;

    private Long lastAttemptId;
}
