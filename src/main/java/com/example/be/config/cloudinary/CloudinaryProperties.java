package com.example.be.config.cloudinary;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thuộc tính cấu hình tích hợp dịch vụ lưu trữ đám mây Cloudinary.
 * Đọc từ prefix 'cloudinary' trong application.properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    /**
     * Tên định danh đám mây Cloudinary (Cloud Name).
     */
    private String cloudName;

    /**
     * Khóa API định danh tài khoản.
     */
    private String apiKey;

    /**
     * Khóa bí mật phục vụ tạo chữ ký bảo mật phía Backend (API Secret).
     */
    private String apiSecret;

    /**
     * Thời gian hiệu lực của chữ ký tải lên (tính bằng giây, mặc định 3600s).
     */
    private Integer signatureExpiresIn = 3600;
}
