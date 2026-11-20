package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dòng nhật ký hoạt động gần đây của học viên trên nền tảng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String avatar;
    private String action;
    private String aimTarget;
    private String timeAgo;
    private LocalDateTime timestamp;
    private String resultBadge;
    private Integer score;
}
