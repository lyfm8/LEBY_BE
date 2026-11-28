package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đại diện cho một Mô-đun học tập hiển thị trên Thư viện khóa học 7 Part (UC17 Catalog).
 * Kết hợp thông tin học thuật của Mô-đun với ngữ cảnh lộ trình cá nhân của học viên
 * để phân biệt rõ "Trọng tâm cần học" (trong lộ trình) và "Đã nắm vững • Học tự do" (ngoài lộ trình).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogModuleItemResponse {

    private Long id;

    private Integer sequence;

    private String title;

    private String description;

    private Long partId;

    private String partName;

    private ESection section;

    private List<String> abilities;

    private Boolean inRoadmap;

    private Integer roadmapOrderNo;

    private String roadmapReason;

    private ECatalogAccessMode accessMode;

    private String categoryLabel;

    private ELessonStatus status;

    private String statusLabel;

    private Boolean canAccess;

    private String lockReason;

    private Integer progressPercent;

    private Integer totalLessons;

    private Integer completedLessons;

    private Integer videoLessonCount;

    private Integer practiceLessonCount;

    private Integer totalDurationSeconds;

    private List<CatalogLessonPreviewResponse> lessonPreviews;
}
