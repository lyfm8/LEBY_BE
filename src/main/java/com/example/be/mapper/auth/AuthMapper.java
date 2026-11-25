package com.example.be.mapper.auth;

import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.user.ELearnerType;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity User và DTO AuthResponse.
 */
@Component
public class AuthMapper {

    /**
     * Ánh xạ thông tin User và mục tiêu AIM hiện tại (nếu có) sang AuthResponse.
     *
     * @param user Thực thể người dùng đã được xác thực
     * @return AuthResponse chứa thông tin tài khoản và trạng thái đã chọn mục tiêu AIM hay chưa
     */
    public AuthResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        String roleName = user.getRole() != null ? user.getRole().name() : null;
        TargetProfile targetProfile = user.getTargetProfile();

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dob(user.getDob())
                .avatar(user.getAvatar())
                .role(roleName)
                .learnerType(user.getLearnerType())
                .hasSelectedTarget(targetProfile != null)
                .targetProfileId(targetProfile != null ? targetProfile.getId() : null)
                .targetTotalScore(targetProfile != null ? targetProfile.getTargetTotalScore() : null)
                .build();
    }
}
