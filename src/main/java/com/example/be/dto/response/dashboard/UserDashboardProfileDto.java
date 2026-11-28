package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO thông tin tài khoản cơ bản và mốc điểm TOEIC mục tiêu (AIM) trên Dashboard học viên (UC08).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardProfileDto {

    private String fullName;
    private String avatar;
    private String role;
    private Long targetProfileId;
    private Integer targetScore;
    private String targetDescription;
}
