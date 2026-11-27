package com.example.be.dto.response.content;

import com.example.be.dto.request.content.LessonScriptDto;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chi tiết đầy đủ của một bài học (Lesson), bao gồm kịch bản phụ đề (scripts)
 * và bộ câu hỏi luyện tập (practiceQuestions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDetailResponse {

    private Long id;
    private Long moduleId;
    private String moduleTitle;
    private Long partId;
    private String partName;
    private ESection section;
    private String title;
    private Integer orderNo;
    private EModuleStatus status;
    private ELessonType lessonType;
    private Integer durationSeconds;
    private Boolean isFree;
    private String descriptions;
    private Long abilityId;
    private String abilityName;
    private String audioUrl;
    private String videoUrl;
    private String imageUrl;
    private List<LessonScriptDto> scripts;
    private List<PracticeQuestionResponse> practiceQuestions;
    private int practiceQuestionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

