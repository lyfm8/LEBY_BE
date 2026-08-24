package com.example.be.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Caffeine In-Memory Cache.
 *
 * Cache "otpCache" được dùng bởi OtpService để lưu mã OTP 6 số.
 * TTL: 10 phút (hết hạn tính từ lúc ghi vào Cache).
 * Max size: 1000 entries (đủ cho số lượng người dùng đồng thời vừa phải).
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("otpCache");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1000)
        );
        return manager;
    }
}
