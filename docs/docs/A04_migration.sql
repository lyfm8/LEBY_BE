-- =============================================================================
-- Migration DDL: UC A04 - Quản lý Ngân hàng câu hỏi & Question Medias
-- Dự án: LEBY
-- =============================================================================

-- 1. Thêm cột order_no vào bảng questions (hỗ trợ sắp xếp câu con trong chùm)
ALTER TABLE questions ADD COLUMN IF NOT EXISTS order_no INT NULL;

-- 2. Thêm cột original_filename và duration vào bảng medias
ALTER TABLE medias ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255) NULL;
ALTER TABLE medias ADD COLUMN IF NOT EXISTS duration DOUBLE NULL;

-- 3. Tạo bảng liên kết question_medias (Quan hệ 1-N giữa Question và Media)
CREATE TABLE IF NOT EXISTS question_medias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    order_no INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qm_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_qm_media FOREIGN KEY (media_id) REFERENCES medias(id),
    CONSTRAINT uk_question_media UNIQUE (question_id, media_id)
);

-- 4. Đánh chỉ mục hiệu năng cho bảng questions
CREATE INDEX IF NOT EXISTS idx_questions_part_id ON questions (part_id);
CREATE INDEX IF NOT EXISTS idx_questions_parent_id ON questions (parent_id);
CREATE INDEX IF NOT EXISTS idx_questions_type ON questions (type);
CREATE INDEX IF NOT EXISTS idx_questions_difficulty ON questions (difficulty);

-- 5. Xóa các cột media đơn lẻ cũ nếu đã tồn tại
ALTER TABLE questions DROP COLUMN IF EXISTS audio_url;
ALTER TABLE questions DROP COLUMN IF EXISTS video_url;
ALTER TABLE questions DROP COLUMN IF EXISTS image_url;

