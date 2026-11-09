package com.example.be.dto.request.media;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Yêu cầu lưu thông tin tệp tin media sau khi client đã tải trực tiếp lên Cloudinary thành công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMediaRequest {

    @JsonProperty("public_id")
    @NotBlank(message = "public_id không được để trống")
    @Size(max = 255, message = "public_id không được vượt quá 255 ký tự")
    private String publicId;

    @JsonProperty("secure_url")
    @NotBlank(message = "secure_url không được để trống")
    @Pattern(regexp = "^https://res\\.cloudinary\\.com/.*$", message = "secure_url phải là URL HTTPS hợp lệ của Cloudinary")
    private String secureUrl;

    @JsonProperty("resource_type")
    @NotBlank(message = "resource_type không được để trống")
    @Pattern(regexp = "^(?i)(image|video|raw|auto)$", message = "resource_type chỉ chấp nhận image, video, raw hoặc auto")
    private String resourceType;

    @NotBlank(message = "format không được để trống")
    @Size(max = 50, message = "format không được vượt quá 50 ký tự")
    private String format;

    @NotNull(message = "bytes không được để trống")
    @Positive(message = "bytes phải lớn hơn 0")
    private Long bytes;

    @Positive(message = "width phải lớn hơn 0")
    private Integer width;

    @Positive(message = "height phải lớn hơn 0")
    private Integer height;

    @JsonProperty("original_filename")
    @Size(max = 255, message = "original_filename không được vượt quá 255 ký tự")
    private String originalFilename;

    @Positive(message = "duration phải lớn hơn 0")
    private Double duration;
}
