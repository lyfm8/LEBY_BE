package com.example.be.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Mục đích của OTP: "REGISTER" hoặc "FORGOT_PASSWORD".
     * Được validate thủ công trong AuthService để tránh expose enum detail qua API.
     */
    @NotBlank(message = "Purpose is required")
    private String purpose;
}
