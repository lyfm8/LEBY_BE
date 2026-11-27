package com.example.be.dto.response.content;

import com.example.be.enums.content.ELessonType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một bài giảng lý thuyết hoặc bài tập thực hành bên trong trang Chi tiết Module (UC10).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModuleLessonResponse {

    private Long id;

    private Integer orderNo;

    private String title;

    private ELessonType type;

    private Integer durationSeconds;

    /**
     * Số lượng câu hỏi gắn trong practice_questions (áp dụng cho cả câu ôn tập của VIDEO và câu luyện tập của PRACTICE).
     */
    private Integer questionCount;

    @JsonProperty("isFree")
    private Boolean isFree;

    @JsonProperty("isCompleted")
    private Boolean isCompleted;

    @JsonProperty("isLocked")
    private Boolean isLocked;

    private String abilityName;
}
