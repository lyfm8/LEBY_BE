package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp toàn bộ dữ liệu Hồ sơ cá nhân của người dùng (UC15).
 * Bao gồm thông tin tài khoản, mục tiêu AIM và ma trận năng lực tích lũy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UserInfoResponse user;
    private UserTargetResponse target;
    private List<UserSkillStatResponse> skills;
}
