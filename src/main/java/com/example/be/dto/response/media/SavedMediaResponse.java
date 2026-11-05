package com.example.be.dto.response.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String format;
    private Long bytes;
    private Integer width;
    private Integer height;
    private String status;
}
