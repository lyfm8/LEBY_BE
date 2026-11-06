package com.example.be.dto.response.content;

import com.example.be.enums.content.EModuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tóm tắt bài học (Lesson) phục vụ hiển thị trên cây danh mục bên trái.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSummaryResponse {

    private Long id;
    private String title;
    private Integer orderNo;
    private EModuleStatus status;
    private Boolean isFree;
    private Long abilityId;
    private String abilityName;
    private String audioUrl;
    private String videoUrl;
    private String imageUrl;
}

