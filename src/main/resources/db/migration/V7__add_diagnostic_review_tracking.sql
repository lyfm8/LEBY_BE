-- =====================================================================================
-- V7: Bổ sung theo dõi trạng thái đã xem đáp án Review trên bảng diagnostic_attempts
-- Phục vụ quy tắc khóa quyền làm lại bài chẩn đoán sau khi đã mở xem đáp án chi tiết.
-- =====================================================================================

ALTER TABLE diagnostic_attempts
    ADD COLUMN has_viewed_review BIT(1) NOT NULL DEFAULT b'0';

ALTER TABLE diagnostic_attempts
    ADD COLUMN review_viewed_at DATETIME(6) NULL;
