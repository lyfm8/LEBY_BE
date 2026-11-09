package com.example.be.dto.response.question;

import com.example.be.enums.media.EMediaKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi thông tin tệp đa phương tiện liên kết với câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMediaResponse {

    private Long mediaId;
    private String publicId;
    private String url;
    private EMediaKind kind;
    private String format;
    private Long bytes;
    private String originalFilename;
    private Double duration;
    private Integer width;
    private Integer height;
    private Integer orderNo;
}

