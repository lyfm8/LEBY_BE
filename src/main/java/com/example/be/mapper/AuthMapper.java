package com.example.be.mapper;

import com.example.be.dto.response.auth.AuthResponse;
import com.example.be.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    public AuthResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .dob(user.getDob())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}
