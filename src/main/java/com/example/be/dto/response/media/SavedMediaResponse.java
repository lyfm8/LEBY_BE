package com.example.be.dto.response.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phản hồi chi tiết sau khi bản ghi media được lưu trữ trong hệ thống LEBY.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedMediaResponse {

    private Long id;
    private String publicId;
    private String url;
    private String resourceType;
    private String kind;
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
    private String originalFilename;
    private Double duration;
    private String status;
    private LocalDateTime createdAt;
}
