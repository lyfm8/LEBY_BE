package com.example.be.service.auth.implement;

import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.service.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email chứa mã OTP 6 số cho người dùng.
     *
     * @param toEmail Email người nhận.
     * @param otp     Mã OTP 6 số.
     * @param isRegister true nếu dùng cho đăng ký, false nếu dùng cho quên mật khẩu.
     */
    @Override
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

    /**
     * Gửi email chứa mật khẩu mới sau khi Quản trị viên thực hiện đặt lại mật khẩu (UC A02).
     * Ném BaseException khi lỗi để caller rollback toàn bộ thay đổi trong DB.
     *
     * @param toEmail     Email tài khoản nhận mật khẩu mới
     * @param fullName    Họ tên người dùng
     * @param newPassword Mật khẩu ngẫu nhiên mới
     * @throws BaseException Khi quá trình gửi email qua SMTP thất bại
     */
    @Override
    public void sendAdminResetPasswordEmail(String toEmail, String fullName, String newPassword) {
        try {
            String recipientName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "bạn";
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[LEBY] Thông báo đặt lại mật khẩu tài khoản");
            message.setText(buildResetPasswordEmailBody(recipientName, newPassword));

            // Gửi đồng bộ để bắt chính xác lỗi SMTP trước khi commit transaction đổi mật khẩu
            mailSender.send(message);
            log.info("[MAIL] Admin reset password email sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("[MAIL] Failed to send reset password email to {}: {}", toEmail, ex.getMessage());
            throw new BaseException(
                    ErrorCode.MAIL_SEND_FAILED,
                    "Không thể gửi email mật khẩu mới đến " + toEmail + ". Mật khẩu tài khoản vẫn được giữ nguyên."
            );
        }
    }

    private String buildResetPasswordEmailBody(String recipientName, String newPassword) {
        return String.format(
                "Xin chào %s,\n\n" +
                "Quản trị viên hệ thống LEBY vừa thực hiện đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                "Mật khẩu đăng nhập mới của bạn là:\n\n" +
                "    %s\n\n" +
                "Toàn bộ phiên đăng nhập trước đó trên các thiết bị đã được thu hồi để đảm bảo an toàn.\n" +
                "Vui lòng đăng nhập bằng mật khẩu mới và đổi lại mật khẩu cá nhân trong mục Hồ sơ.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ LEBY",
                recipientName, newPassword
        );
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
