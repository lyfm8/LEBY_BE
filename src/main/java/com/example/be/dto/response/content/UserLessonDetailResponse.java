package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO hoàn chỉnh cho màn hình Phòng học bài giảng Video của học viên (UC11).
 * Bao gồm thông tin học liệu, tiến độ xem tích lũy, điều hướng trong Module và câu hỏi ôn tập củng cố.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLessonDetailResponse {

    private Long id;
    private Long moduleId;
    private String moduleTitle;
    private String partName;
    private ESection section;
    private String title;
    private Integer orderNo;
    private ELessonType lessonType;
    private String descriptions;
    private String videoUrl;
    private String audioUrl;
    private String imageUrl;
    private Integer durationSeconds;
    private Boolean isFree;
    private String abilityName;

    // Thông tin tiến độ xem của học viên đối với bài giảng hiện tại
    private Integer watchedSeconds;
    private Integer lastPositionSeconds;
    private Integer watchPercent;
    private Integer requiredWatchPercent;
    private Boolean isCompleted;
    private LocalDateTime completedAt;

    // Tiến độ tổng hợp và điều hướng trong cùng Module
    private Integer moduleProgressPercent;
    private Integer completedVideoLessons;
    private Integer totalVideoLessons;
    private Integer requiredCompletionRate;
    private Boolean moduleTestUnlocked;
    private Long prevLessonId;
    private Long nextLessonId;
    private ELessonType nextLessonType;
    private List<UserLessonNavItemResponse> moduleLessons;

    // Bộ câu hỏi trắc nghiệm ôn tập củng cố gắn vào bài giảng (nếu có)
    private List<UserLessonReviewQuestionResponse> reviewQuestions;
}
