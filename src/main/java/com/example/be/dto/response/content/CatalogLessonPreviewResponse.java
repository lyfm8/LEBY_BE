package com.example.be.dto.response.content;

import com.example.be.enums.content.ELessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đề cương xem trước của một bài học trên Thư viện khóa học 7 Part (UC17 Catalog).
 * Tuân thủ nghiêm ngặt BR13: chỉ trả về metadata đề cương (thứ tự, tiêu đề, loại bài, thời lượng, miễn phí),
 * tuyệt đối không trả về videoUrl/audioUrl hay nội dung câu hỏi để bảo vệ học liệu khi mô-đun đang khóa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogLessonPreviewResponse {

    private Long id;

    private Integer orderNo;

    private String title;

    private ELessonType type;

    private Integer durationSeconds;

    private Boolean isFree;
}
