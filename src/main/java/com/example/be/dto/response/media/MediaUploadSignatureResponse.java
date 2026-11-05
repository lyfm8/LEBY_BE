package com.example.be.dto.response.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi chứa chữ ký bảo mật và thông tin tài khoản Cloudinary để Frontend thực hiện tải tệp tin trực tiếp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadSignatureResponse {

    /**
     * Chữ ký mã hóa xác thực request tải tệp tin.
     */
    private String signature;

    /**
     * Dấu thời gian Unix Epoch (giây) tại thời điểm ký.
     */
    private Long timestamp;

    /**
     * Khóa công khai API Key của tài khoản Cloudinary.
     */
    private String apiKey;

    /**
     * Tên không gian lưu trữ Cloud Name trên Cloudinary.
     */
    private String cloudName;

    /**
     * Thời gian hiệu lực của chữ ký (tính bằng giây).
     */
    private Integer expiresIn;
}
