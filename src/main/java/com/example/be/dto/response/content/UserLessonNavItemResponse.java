package com.example.be.dto.response.content;

import com.example.be.enums.content.ELessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO tóm tắt từng bài học trong thanh điều hướng của phòng học Video (UC11).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLessonNavItemResponse {

    private Long id;
    private Integer orderNo;
    private String title;
    private ELessonType lessonType;
    private Integer durationSeconds;
    private Boolean isCompleted;
    private Integer watchPercent;
}
