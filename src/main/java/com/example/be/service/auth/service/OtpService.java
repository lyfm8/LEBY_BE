package com.example.be.service.auth.service;

import com.example.be.enums.auth.OtpPurpose;

/**
 * Interface định nghĩa contract cho OtpService.
 */
public interface OtpService {

    String generateAndStore(String email, OtpPurpose purpose);

    void verify(String email, OtpPurpose purpose, String inputOtp);
}
