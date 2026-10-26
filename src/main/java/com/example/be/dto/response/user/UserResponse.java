package com.example.be.dto.response.user;

import com.example.be.entity.user.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private LocalDateTime createdAt;


}
