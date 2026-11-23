-- =====================================================================================
-- V5: Tạo bảng lesson_progress lưu tiến độ học bài giảng Video của từng học viên (UC11)
-- Phục vụ quy tắc BR09 (xem tích lũy >= 60% thời lượng), Resume vị trí xem, và BR03
-- =====================================================================================

CREATE TABLE lesson_progress (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    watched_seconds INT NOT NULL DEFAULT 0,
    last_position_seconds INT NOT NULL DEFAULT 0,
    watch_percent INT NOT NULL DEFAULT 0,
    is_completed BIT(1) NOT NULL DEFAULT b'0',
    completed_at DATETIME(6) NULL,
    last_synced_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_progress_user_lesson (user_id, lesson_id),
    KEY idx_lesson_progress_user_id (user_id),
    KEY idx_lesson_progress_lesson_id (lesson_id),
    KEY idx_lesson_progress_user_completed (user_id, is_completed),
    CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
