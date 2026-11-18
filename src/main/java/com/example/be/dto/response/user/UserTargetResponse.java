package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phản hồi thông tin mốc mục tiêu TOEIC (AIM) của người dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTargetResponse {

    private Long targetProfileId;
    private Integer targetTotalScore;
    private String description;
    private LocalDateTime assignedAt;
}
