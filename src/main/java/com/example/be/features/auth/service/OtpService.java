package com.example.be.features.auth.service;

import com.example.be.common.exception.BaseException;
import com.example.be.common.exception.ErrorCode;
import com.example.be.features.auth.enums.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Quản lý vòng đời OTP 6 số sử dụng Caffeine In-Memory Cache.
 *
 * Cache key: "{purpose}_{email}" — ví dụ: "REGISTER_user@example.com"
 * TTL: 10 phút (cấu hình trong application.properties: spring.cache.caffeine.spec)
 *
 * Luồng:
 * 1. generateAndStore() → sinh OTP, lưu vào Cache, trả về mã.
 * 2. verify()           → lấy từ Cache, so sánh, xóa nếu đúng.
 */
@Slf4j
@Service
public class OtpService {

    private static final String CACHE_NAME = "otpCache";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CacheManager cacheManager;

    public OtpService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Sinh mã OTP 6 số, lưu vào Caffeine Cache.
     *
     * @param email   Email của user (dùng làm key, kết hợp với purpose).
     * @param purpose Mục đích của OTP (REGISTER hoặc FORGOT_PASSWORD).
     * @return Mã OTP 6 số dạng String (có thể bắt đầu bằng 0).
     */
    public String generateAndStore(String email, OtpPurpose purpose) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String key = buildKey(email, purpose);

        Cache cache = getCache();
        cache.put(key, otp);

        log.debug("[OTP] Stored OTP for key={}", key);
        return otp;
    }

    /**
     * Xác minh OTP người dùng nhập.
     * Nếu đúng: xóa OTP khỏi Cache để không dùng lại được.
     * Nếu sai hoặc không tồn tại: throw BaseException.
     *
     * @param email     Email của user.
     * @param purpose   Mục đích của OTP.
     * @param inputOtp  Mã OTP người dùng nhập vào.
     * @throws BaseException OTP_NOT_FOUND nếu OTP không tồn tại hoặc đã hết hạn.
     * @throws BaseException OTP_INVALID nếu OTP sai.
     */
    public void verify(String email, OtpPurpose purpose, String inputOtp) {
        String key = buildKey(email, purpose);
        Cache cache = getCache();

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) {
            // Không tìm thấy trong Cache: hoặc chưa gửi, hoặc đã hết 10 phút.
            throw new BaseException(ErrorCode.OTP_NOT_FOUND);
        }

        String storedOtp = (String) wrapper.get();
        if (!inputOtp.equals(storedOtp)) {
            throw new BaseException(ErrorCode.OTP_INVALID);
        }

        // Xóa OTP sau khi xác minh thành công — không thể dùng lại.
        cache.evict(key);
        log.debug("[OTP] Verified and evicted OTP for key={}", key);
    }

    private String buildKey(String email, OtpPurpose purpose) {
        return purpose.name() + "_" + email;
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + CACHE_NAME + "' not found. Check Caffeine configuration.");
        }
        return cache;
    }
}
