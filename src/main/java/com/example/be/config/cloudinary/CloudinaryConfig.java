package com.example.be.config.cloudinary;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Cấu hình khởi tạo Bean Cloudinary dùng chung trong toàn hệ thống LEBY.
 */
@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    /**
     * Khởi tạo đối tượng Cloudinary SDK với thông tin xác thực từ cấu hình.
     *
     * @param properties Thuộc tính cấu hình Cloudinary nạp từ application.properties
     * @return Cloudinary Đối tượng Client SDK sẵn sàng thực hiện các thao tác ký và quản lý media
     */
    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(Map.of(
                "cloud_name", properties.getCloudName() != null ? properties.getCloudName() : "",
                "api_key", properties.getApiKey() != null ? properties.getApiKey() : "",
                "api_secret", properties.getApiSecret() != null ? properties.getApiSecret() : "",
                "secure", true
        ));
    }
}
