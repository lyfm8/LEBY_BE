package com.example.be.service.media.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.be.config.cloudinary.CloudinaryProperties;
import com.example.be.dto.response.media.MediaUploadSignatureResponse;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter chuyển hợp đồng quản lý lưu trữ Media sang thư viện Cloudinary SDK.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryMediaStorageAdapter implements MediaStorageAdapter {

    private static final String TEMPORARY_TAG = "tmp";
    private static final String CLOUDINARY_RESULT_OK = "ok";
    private static final String CLOUDINARY_RESULT_NOT_FOUND = "not found";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    /**
     * Tạo chữ ký bảo mật để Frontend tải trực tiếp lên Cloudinary.
     * Chỉ ký các trường FE thực sự gửi để tránh lỗi chữ ký không khớp.
     *
     * @return MediaUploadSignatureResponse Dữ liệu chữ ký và thông số cần thiết
     */
    @Override
    public MediaUploadSignatureResponse createUploadSignature() {
        validateCloudinaryConfiguration();

        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> parametersToSign = new HashMap<>();
        parametersToSign.put("timestamp", timestamp);
        parametersToSign.put("tags", TEMPORARY_TAG);

        String signature = cloudinary.apiSignRequest(parametersToSign, cloudinaryProperties.getApiSecret());

        return MediaUploadSignatureResponse.builder()
                .signature(signature)
                .timestamp(timestamp)
                .apiKey(cloudinaryProperties.getApiKey())
                .cloudName(cloudinaryProperties.getCloudName())
                .expiresIn(cloudinaryProperties.getSignatureExpiresIn())
                .build();
    }

    /**
     * Xóa tài nguyên trên Cloudinary theo public_id.
     *
     * @param publicId Mã định danh tài nguyên trên Cloudinary
     * @param resourceType Loại tài nguyên (image, video, raw)
     * @return boolean true nếu xóa thành công hoặc không tìm thấy file
     */
    @Override
    public boolean deleteResource(String publicId, String resourceType) {
        try {
            String type = StringUtils.hasText(resourceType) ? resourceType.toLowerCase() : "image";
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", type,
                            "invalidate", true
                    )
            );

            Object status = result.get("result");
            return CLOUDINARY_RESULT_OK.equals(status) || CLOUDINARY_RESULT_NOT_FOUND.equals(status);
        } catch (Exception ex) {
            log.warn("Không thể xóa tài nguyên Cloudinary publicId={}: {}", publicId, ex.getMessage());
            return false;
        }
    }

    /**
     * Xóa tag khỏi tài nguyên trên Cloudinary (ví dụ bỏ tag tmp sau khi chốt dữ liệu).
     *
     * @param tag Tên tag cần xóa
     * @param publicId Mã định danh tài nguyên
     */
    @Override
    public void removeTag(String tag, String publicId) {
        try {
            cloudinary.uploader().removeTag(
                    tag,
                    new String[]{publicId},
                    ObjectUtils.emptyMap()
            );
        } catch (Exception ex) {
            log.warn("Không thể xóa tag '{}' cho tài nguyên Cloudinary publicId={}: {}",
                    tag, publicId, ex.getMessage());
        }
    }

    private void validateCloudinaryConfiguration() {
        if (!StringUtils.hasText(cloudinaryProperties.getCloudName())
                || !StringUtils.hasText(cloudinaryProperties.getApiKey())
                || !StringUtils.hasText(cloudinaryProperties.getApiSecret())) {
            throw new BaseException(
                    ErrorCode.MEDIA_CONFIG_INVALID,
                    "Cloudinary chưa được cấu hình đầy đủ trong hệ thống (thiếu cloudName/apiKey/apiSecret)"
            );
        }
    }
}
