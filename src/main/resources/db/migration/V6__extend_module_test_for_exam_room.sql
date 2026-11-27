-- =====================================================================================
-- V6: Mở rộng bảng module_test_attempts và module_test_answers phục vụ Phòng thi vượt ải (UC13 - UC14)
-- (Đánh số V6 vì V5 đã được sử dụng bởi V5__create_lesson_progress_table.sql trên dev)
-- Tách từng ALTER TABLE độc lập để tương thích hoàn toàn với TiDB Cloud & MySQL 8.0.
-- =====================================================================================

ALTER TABLE module_test_attempts
    ADD COLUMN started_at DATETIME(6) NULL,
    ADD COLUMN remaining_seconds INT NULL,
    ADD COLUMN duration_seconds INT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    ADD COLUMN question_snapshot JSON NULL;

UPDATE module_test_attempts
SET status = 'SUBMITTED'
WHERE is_submitted = b'1';

ALTER TABLE module_test_answers
    MODIFY COLUMN module_test_question_id BIGINT NULL;

ALTER TABLE module_test_answers
    ADD COLUMN question_id BIGINT NULL,
    ADD COLUMN order_no INT NULL;

UPDATE module_test_answers mta
JOIN module_test_questions mtq ON mtq.id = mta.module_test_question_id
SET mta.question_id = mtq.question_id,
    mta.order_no = mtq.order_no
WHERE mta.question_id IS NULL;

ALTER TABLE module_test_answers
    MODIFY COLUMN question_id BIGINT NOT NULL;

ALTER TABLE module_test_answers
    ADD CONSTRAINT fk_module_test_answers_question_direct
        FOREIGN KEY (question_id) REFERENCES questions (id);

ALTER TABLE module_test_answers
    ADD UNIQUE KEY uk_module_test_answer_attempt_question (module_test_attempt_id, question_id);
