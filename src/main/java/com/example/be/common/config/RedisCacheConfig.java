package com.example.be.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Cấu hình Redis Distributed Cache.
 *
 * Cache "otpCache" được dùng bởi OtpService để lưu mã OTP 6 số.
 * TTL: 10 phút (hết hạn tính từ lúc ghi vào Cache).
 * Serializer: String (vì OTP là chuỗi 6 ký tự số, không cần JSON/binary).
 *
 * Key trong Redis có dạng: "otpCache::REGISTER_user@example.com"
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration otpCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(Map.of("otpCache", otpCacheConfig))
                .build();
    }
}
