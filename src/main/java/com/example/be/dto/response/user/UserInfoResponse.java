package com.example.be.dto.response.user;

import com.example.be.enums.user.ELearnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phản hồi thông tin hồ sơ cơ bản của người dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String email;
    private String fullName;
    private LocalDate dob;
    private String avatar;
    private ELearnerType learnerType;
    private String role;
    private LocalDateTime createdAt;
}
