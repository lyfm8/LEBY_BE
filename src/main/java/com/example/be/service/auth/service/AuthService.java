package com.example.be.service.auth.service;

import com.example.be.dto.request.auth.SendOtpRequest;
import com.example.be.dto.request.auth.RegisterRequest;
import com.example.be.dto.request.auth.LoginRequest;
import com.example.be.dto.response.auth.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface định nghĩa contract cho AuthService.
 */
public interface AuthService {

    void sendOtp(SendOtpRequest request);

    AuthResponse register(RegisterRequest request, HttpServletResponse response);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    void refresh(HttpServletRequest request, HttpServletResponse response);

    void logout(Long userId, HttpServletResponse response);
}
