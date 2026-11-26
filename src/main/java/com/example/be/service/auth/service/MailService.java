package com.example.be.service.auth.service;

/**
 * Interface định nghĩa contract cho MailService.
 */
public interface MailService {

    void sendOtpEmail(String toEmail, String otp, boolean isRegister);

    /**
     * Gửi email chứa mật khẩu mới do quản trị viên đặt lại.
     * Phải ném ngoại lệ nếu gửi thất bại để transaction đặt lại mật khẩu tự động rollback.
     *
     * @param toEmail     Địa chỉ email của tài khoản
     * @param fullName    Họ tên người dùng để cá nhân hóa lời chào
     * @param newPassword Mật khẩu ngẫu nhiên mới chưa mã hóa
     */
    void sendAdminResetPasswordEmail(String toEmail, String fullName, String newPassword);
}
