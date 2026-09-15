package com.example.be.service.auth.service;

/**
 * Interface định nghĩa contract cho MailService.
 */
public interface MailService {

    void sendOtpEmail(String toEmail, String otp, boolean isRegister);
}
