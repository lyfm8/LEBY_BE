package com.example.be.dto.response.dashboard;

import com.example.be.enums.content.ELessonType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO biểu diễn một bài giảng Video hoặc bài Luyện tập hiển thị trong khối mô-đun hiện tại trên Dashboard (UC08 Bước 5).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardLessonItemDto {

    private Long id;
    private Integer orderNo;
    private String title;
    private ELessonType type;
    private int durationSeconds;
    private int questionCount;
    private int watchPercent;

    @JsonProperty("isCompleted")
    private Boolean isCompleted;

    @JsonProperty("isNextRecommended")
    private Boolean isNextRecommended;
}
