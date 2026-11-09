package com.example.be.mapper.auth;

import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.entity.user.User;
import com.example.be.enums.user.ERole;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity User và DTO AuthResponse.
 */
@Component
public class AuthMapper {

    public AuthResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        String roleName = null;
        if (user.getRole() != null) {
            // Chuẩn hóa role STUDENT sang USER để tương thích chặt chẽ với Frontend AuthGuard
            roleName = user.getRole() == ERole.STUDENT ? "USER" : user.getRole().name();
        }
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dob(user.getDob())
                .avatar(user.getAvatar())
                .role(roleName)
                .build();
    }
}
