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

    private Integer bestScore;

    private Long lastAttemptId;
}
