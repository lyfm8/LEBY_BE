-- =====================================================================================
-- V4: Bổ sung 4 cột cấu hình Bài thi vượt ải (Module Test - UC A07) vào bảng modules
-- Đồng bộ test_question_count với số câu hỏi thực tế đã seed trong module_test_questions (V2)
-- =====================================================================================

ALTER TABLE modules
    ADD COLUMN test_duration_minutes INT NOT NULL DEFAULT 25,
    ADD COLUMN test_question_count INT NOT NULL DEFAULT 25,
    ADD COLUMN shuffle_questions BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN test_selection_mode VARCHAR(30) NOT NULL DEFAULT 'MANUAL';

-- Đồng bộ lại số câu hỏi mục tiêu cho các module đã có sẵn đề thi trong V2 seed data
UPDATE modules m
SET m.test_question_count = (
    SELECT COUNT(*)
    FROM module_test_questions mtq
    WHERE mtq.module_id = m.id
)
WHERE EXISTS (
    SELECT 1
    FROM module_test_questions mtq
    WHERE mtq.module_id = m.id
);
