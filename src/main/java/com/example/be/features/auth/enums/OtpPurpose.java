package com.example.be.features.auth.enums;

/**
 * Mục đích sử dụng của OTP.
 * Ngăn việc dùng OTP của luồng này cho luồng khác (ví dụ: OTP đăng ký dùng cho quên mật khẩu).
 */
public enum OtpPurpose {
    REGISTER,
    FORGOT_PASSWORD
}
