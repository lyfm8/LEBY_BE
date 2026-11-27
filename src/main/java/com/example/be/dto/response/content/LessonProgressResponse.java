package com.example.be.dto.response.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO phản hồi sau mỗi nhịp đồng bộ tiến độ xem Video (PUT /api/users/lessons/{lessonId}/progress).
 * Cung cấp trạng thái hoàn thành bài giảng (BR09) và tín hiệu mở khóa bài thi vượt ải Module (BR03).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressResponse {

    private Long lessonId;
    private Integer watchedSeconds;
    private Integer lastPositionSeconds;
    private Integer durationSeconds;
    private Integer watchPercent;
    private Integer requiredWatchPercent;
    private Boolean isCompleted;
    private Boolean justCompleted;
    private LocalDateTime completedAt;
    private Integer moduleProgressPercent;
    private Integer completedVideoLessons;
    private Integer totalVideoLessons;
    private Integer requiredCompletionRate;
    private Boolean moduleTestUnlocked;
    private Boolean moduleTestJustUnlocked;
}
