package com.example.be.features.auth.dto.response;

import com.example.be.features.user.entity.User;
import lombok.Getter;

/**
 * Thông tin user trả về sau khi đăng nhập hoặc đăng ký thành công.
 * Không chứa password, tokenVersion hay thông tin nhạy cảm.
 */
@Getter
public class AuthResponse {

    private final Long id;
    private final String email;
    private final String username;
    private final String fullName;
    private final String avatar;
    private final String role;

    public AuthResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.avatar = user.getAvatar();
        this.role = user.getRole().name();
    }
}
