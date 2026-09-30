package com.example.be.service.user;

import com.example.be.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(String keyword, String role, Boolean isActive, Pageable pageable);
    UserResponse getUserById(Long id);
    void toggleUserStatus(Long id);
}
