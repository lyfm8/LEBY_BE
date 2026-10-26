package com.example.be.service.user.implement;

import com.example.be.service.user.service.UserService;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.enums.user.ERole;
import com.example.be.entity.user.User;
import com.example.be.mapper.UserMapper;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getAllUsers(String keyword, String role, Boolean isActive, Pageable pageable) {
        ERole roleEnum = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleEnum = ERole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid role
            }
        }
        
        Page<User> users = userRepository.findUsersByFilters(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                roleEnum,
                isActive,
                pageable
        );
        return users.map(userMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        user.setIsActive(!user.getIsActive());
        
        // Khi khóa tài khoản, tăng tokenVersion để vô hiệu hóa toàn bộ token hiện tại của user đó
        if (!user.getIsActive()) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        
        userRepository.save(user);
    }
}
