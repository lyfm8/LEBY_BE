package com.example.be.dto.response.user;

import com.example.be.enums.user.ELearnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO thông tin tài khoản người dùng trả về cho màn hình Quản lý tài khoản (UC A02).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Boolean isActive;
    private Long targetProfileId;
    private Integer aimScore;
    private String aimTarget;
    private ELearnerType learnerType;
    private LocalDate dob;
    private String avatar;
    private LocalDateTime createdAt;
}
