package com.example.be.service.user.implement;

import com.example.be.service.user.service.UserService;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.enums.user.ERole;
import com.example.be.entity.user.User;
import com.example.be.mapper.user.UserMapper;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        // Bước 1: Tìm tài khoản cần khóa/mở khóa trong cơ sở dữ liệu
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        // Bước 2: Kiểm tra quy tắc BR11 — chặn Admin tự khóa tài khoản của chính mình để tránh mất quyền quản trị
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            boolean isSelfLocking = currentUser.getId() != null
                    && currentUser.getId().equals(id)
                    && Boolean.TRUE.equals(user.getIsActive());
            if (isSelfLocking) {
                throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể tự khóa tài khoản của chính mình");
            }
        }

        // Bước 3: Đảo trạng thái kích hoạt của tài khoản
        user.setIsActive(!user.getIsActive());

        // Bước 4: Khi khóa tài khoản, tăng tokenVersion để vô hiệu hóa toàn bộ JWT hiện tại của user đó
        if (!user.getIsActive()) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }

        userRepository.save(user);
    }
}
