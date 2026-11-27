package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể lưu trữ tiến độ xem bài giảng Video và trạng thái hoàn thành từng bài học của học viên (UC11).
 * Đảm bảo mỗi học viên chỉ có tối đa 1 bản ghi tiến độ cho mỗi bài học qua ràng buộc UNIQUE (user_id, lesson_id).
 */
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = {
                // Chặn trùng lặp bản ghi tiến độ cho cùng một cặp (học viên, bài học)
                @UniqueConstraint(name = "uk_lesson_progress_user_lesson", columnNames = {"user_id", "lesson_id"})
        },
        indexes = {
                @Index(name = "idx_lesson_progress_user_id", columnList = "user_id"),
                @Index(name = "idx_lesson_progress_lesson_id", columnList = "lesson_id"),
                @Index(name = "idx_lesson_progress_user_completed", columnList = "user_id, is_completed")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LessonProgress extends AbstractEntity {

    /**
     * Học viên sở hữu bản ghi tiến độ này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Bài học tương ứng trong mô-đun học tập.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /**
     * Tổng thời lượng xem thực tế tích lũy (tính bằng giây, đã loại trừ các đoạn tua bỏ qua theo BR09).
     */
    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds = 0;

    /**
     * Mốc thời gian dừng xem gần nhất (tính bằng giây) để hỗ trợ tính năng Xem tiếp (Resume).
     */
    @Column(name = "last_position_seconds", nullable = false)
    private Integer lastPositionSeconds = 0;

    /**
     * Tỷ lệ phần trăm xem thực tế tích lũy (0 - 100).
     */
    @Column(name = "watch_percent", nullable = false)
    private Integer watchPercent = 0;

    /**
     * Trạng thái đã hoàn thành bài giảng (tự động chuyển sang true khi watchPercent đạt ngưỡng BR09).
     */
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    /**
     * Thời điểm bài giảng lần đầu được ghi nhận hoàn thành.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Thời điểm đồng bộ tiến độ gần nhất lên máy chủ (phục vụ kiểm soát chống gian lận gửi delta dồn dập).
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
