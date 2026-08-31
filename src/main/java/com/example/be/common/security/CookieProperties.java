package com.example.be.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ cấu hình Cookie từ application.properties.
 * Prefix: cookie.*
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    /** Tên Cookie chứa Access Token. Mặc định: "accessToken". */
    private String accessTokenName;

    /** Tên Cookie chứa Refresh Token. Mặc định: "refreshToken". */
    private String refreshTokenName;
}
