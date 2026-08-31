package com.example.be.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ toàn bộ cấu hình JWT và Cookie từ application.properties vào POJO.
 * Prefix: jwt.*, cookie.*
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Access access = new Access();
    private Refresh refresh = new Refresh();

    @Getter
    @Setter
    public static class Access {
        /** Secret key để ký Access Token. Tối thiểu 32 ký tự. */
        private String secret;
        /** Thời hạn Access Token tính bằng millisecond. Mặc định: 900000 (15 phút). */
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Refresh {
        /** Secret key để ký Refresh Token. Tối thiểu 32 ký tự. */
        private String secret;
        /** Thời hạn Refresh Token tính bằng millisecond. Mặc định: 604800000 (7 ngày). */
        private long expirationMs;
    }
}
