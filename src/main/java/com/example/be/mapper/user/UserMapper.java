package com.example.be.mapper.user;

import com.example.be.dto.response.user.UserResponse;
import com.example.be.entity.user.User;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity User và DTO UserResponse.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .createdAt(user.getCreatedAt())
                .build();
    }
}
