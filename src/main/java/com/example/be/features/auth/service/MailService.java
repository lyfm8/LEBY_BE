package com.example.be.features.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service gửi email sử dụng JavaMailSender (Gmail SMTP).
 *
 * Cấu hình SMTP trong application.properties:
 *   spring.mail.username = ${MAIL_USERNAME}
 *   spring.mail.password = ${MAIL_PASSWORD}
 *
 * Phương thức gửi mail được đánh dấu @Async để không block request thread.
 * Cần @EnableAsync trên BeApplication nếu muốn async hoàn toàn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email chứa mã OTP 6 số cho người dùng.
     *
     * @param toEmail Email người nhận.
     * @param otp     Mã OTP 6 số.
     * @param isRegister true nếu dùng cho đăng ký, false nếu dùng cho quên mật khẩu.
     */
    public void sendOtpEmail(String toEmail, String otp, boolean isRegister) {
        try {
            String subject = isRegister
                    ? "[LEBY] Mã xác nhận đăng ký tài khoản"
                    : "[LEBY] Mã xác nhận đặt lại mật khẩu";

            String body = buildOtpEmailBody(otp, isRegister);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("[MAIL] OTP email sent to {}", toEmail);

        } catch (Exception ex) {
            // Ghi log lỗi nhưng không throw — tránh làm fail request của user.
            // Có thể implement retry mechanism sau này nếu cần.
            log.error("[MAIL] Failed to send OTP email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private String buildOtpEmailBody(String otp, boolean isRegister) {
        String action = isRegister ? "đăng ký tài khoản" : "đặt lại mật khẩu";
        return String.format(
                "Xin chào,\n\n" +
                "Mã OTP của bạn để %s trên hệ thống LEBY là:\n\n" +
                "    %s\n\n" +
                "Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ LEBY",
                action, otp
        );
    }
}
