package com.example.be.service.auth.implement;

import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.enums.auth.OtpPurpose;
import com.example.be.service.auth.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Quản lý vòng đời OTP 6 số sử dụng Caffeine In-Memory Cache (BR01).
 *
 * Cache key: "{purpose}_{email}" — ví dụ: "REGISTER_user@example.com"
 * TTL: 10 phút (cấu hình trong application.properties: spring.cache.caffeine.spec)
 *
 * Bảo mật chống brute-force (UC01-6a):
 * - Mỗi mã OTP chỉ cho phép nhập sai tối đa 5 lần; quá 5 lần hệ thống tự hủy mã OTP hiện tại.
 * - Khi gửi lại OTP mới, mã cũ và bộ đếm số lần sai sẽ bị ghi đè/reset ngay lập tức.
 */
@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private static final String CACHE_NAME = "otpCache";
    private static final String ATTEMPT_PREFIX = "ATTEMPT_";
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CacheManager cacheManager;

    public OtpServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Sinh mã OTP 6 số ngẫu nhiên bằng SecureRandom, lưu vào Caffeine Cache (TTL 10 phút)
     * và reset bộ đếm số lần nhập sai của email tương ứng.
     *
     * @param email   Email của user (dùng làm key, kết hợp với purpose).
     * @param purpose Mục đích của OTP (REGISTER hoặc FORGOT_PASSWORD).
     * @return Mã OTP 6 số dạng String (có thể bắt đầu bằng 0).
     */
    @Override
    public String generateAndStore(String email, OtpPurpose purpose) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String key = buildKey(email, purpose);

        Cache cache = getCache();
        // Ghi đè mã OTP mới (tự động vô hiệu hóa mã cũ nếu người dùng bấm "Gửi lại OTP")
        cache.put(key, otp);
        cache.evict(ATTEMPT_PREFIX + key);

        log.debug("[OTP] Stored OTP for key={}", key);
        return otp;
    }

    /**
     * Xác minh OTP người dùng nhập.
     * - Nếu sai quá 5 lần: hủy ngay mã OTP để chống dò mã brute-force và buộc người dùng gửi lại mã mới.
     * - Nếu đúng: xóa ngay OTP khỏi Cache để đảm bảo quy tắc BR01 (mỗi mã chỉ dùng tối đa 1 lần).
     *
     * @param email     Email của user.
     * @param purpose   Mục đích của OTP.
     * @param inputOtp  Mã OTP người dùng nhập vào.
     * @throws BaseException OTP_NOT_FOUND nếu OTP không tồn tại, đã hết 10 phút hoặc bị hủy do sai quá 5 lần.
     * @throws BaseException OTP_INVALID nếu OTP nhập sai.
     */
    @Override
    public void verify(String email, OtpPurpose purpose, String inputOtp) {
        String key = buildKey(email, purpose);
        String attemptKey = ATTEMPT_PREFIX + key;
        Cache cache = getCache();

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) {
            // Không tìm thấy trong Cache: chưa gửi, đã hết hạn 10 phút, hoặc đã bị hủy do sai quá số lần quy định
            throw new BaseException(ErrorCode.OTP_NOT_FOUND);
        }

        String storedOtp = (String) wrapper.get();
        if (!inputOtp.equals(storedOtp)) {
            // SECURITY: Đếm số lần nhập sai; nếu chạm ngưỡng 5 lần thì hủy mã OTP hiện tại chống brute-force
            Cache.ValueWrapper attemptWrapper = cache.get(attemptKey);
            int failedCount = (attemptWrapper != null && attemptWrapper.get() instanceof Integer count) ? count + 1 : 1;

            if (failedCount >= MAX_VERIFY_ATTEMPTS) {
                cache.evict(key);
                cache.evict(attemptKey);
                throw new BaseException(
                        ErrorCode.OTP_INVALID,
                        "Bạn đã nhập sai mã OTP quá 5 lần. Mã OTP đã bị hủy, vui lòng gửi lại mã mới."
                );
            }

            cache.put(attemptKey, failedCount);
            int remaining = MAX_VERIFY_ATTEMPTS - failedCount;
            throw new BaseException(
                    ErrorCode.OTP_INVALID,
                    "Mã OTP không chính xác. Bạn còn " + remaining + " lần thử."
            );
        }

        // Xóa ngay OTP và bộ đếm sau khi xác minh thành công — không thể tái sử dụng (BR01)
        cache.evict(key);
        cache.evict(attemptKey);
        log.debug("[OTP] Verified and evicted OTP for key={}", key);
    }

    private String buildKey(String email, OtpPurpose purpose) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return purpose.name() + "_" + normalizedEmail;
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + CACHE_NAME + "' not found. Check Caffeine configuration.");
        }
        return cache;
    }
}
