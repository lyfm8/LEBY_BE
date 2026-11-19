-- =====================================================================================
-- Migration V4: Thiết kế lại cấu hình Ngưỡng Diagnostic và Quy tắc Năng lực theo AIM (A08 & A10)
-- =====================================================================================

-- 1. Xóa cột full_part_threshold khỏi target_part_thresholds (chỉ giữ pass_score và confirming_score)
ALTER TABLE target_part_thresholds DROP COLUMN full_part_threshold;

-- 2. Tái cấu trúc bảng ability_evaluation_rules:
-- Xóa developing_threshold và min_evidence_count (quy về 1 mốc duy nhất: stable_min)
-- Đổi tên stable_threshold thành stable_min theo quy chuẩn leby-anticonflict.md
ALTER TABLE ability_evaluation_rules
    CHANGE COLUMN stable_threshold stable_min FLOAT NOT NULL,
    DROP COLUMN developing_threshold,
    DROP COLUMN min_evidence_count,
    ADD COLUMN target_profile_id BIGINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT fk_ability_eval_rule_target_profile
        FOREIGN KEY (target_profile_id) REFERENCES target_profiles(id);

-- 3. Xóa ràng buộc Unique cũ (nếu có) và tạo Unique composite (ability_id, target_profile_id)
-- Note: MySQL sinh index theo tên foreign key hoặc unique key
ALTER TABLE ability_evaluation_rules
    ADD CONSTRAINT uk_ability_rule_ability_target UNIQUE (ability_id, target_profile_id);

-- 4. Khởi tạo Seed data chuẩn cho 4 mốc AIM trong hệ thống (AIM 450, 650, 800, 990) × 16 Năng lực
TRUNCATE TABLE ability_evaluation_rules;

-- Profile 1: AIM 450 (stable_min = 60%)
INSERT INTO ability_evaluation_rules (stable_min, ability_id, target_profile_id, created_at, updated_at) VALUES
(60.0, 1, 1, NOW(), NOW()), (60.0, 2, 1, NOW(), NOW()), (60.0, 3, 1, NOW(), NOW()), (60.0, 4, 1, NOW(), NOW()),
(55.0, 5, 1, NOW(), NOW()), (60.0, 6, 1, NOW(), NOW()), (55.0, 7, 1, NOW(), NOW()), (60.0, 8, 1, NOW(), NOW()),
(60.0, 9, 1, NOW(), NOW()), (65.0, 10, 1, NOW(), NOW()), (65.0, 11, 1, NOW(), NOW()), (60.0, 12, 1, NOW(), NOW()),
(55.0, 13, 1, NOW(), NOW()), (60.0, 14, 1, NOW(), NOW()), (60.0, 15, 1, NOW(), NOW()), (55.0, 16, 1, NOW(), NOW());

-- Profile 2: AIM 650 (stable_min = 75%)
INSERT INTO ability_evaluation_rules (stable_min, ability_id, target_profile_id, created_at, updated_at) VALUES
(75.0, 1, 2, NOW(), NOW()), (75.0, 2, 2, NOW(), NOW()), (75.0, 3, 2, NOW(), NOW()), (75.0, 4, 2, NOW(), NOW()),
(70.0, 5, 2, NOW(), NOW()), (75.0, 6, 2, NOW(), NOW()), (70.0, 7, 2, NOW(), NOW()), (75.0, 8, 2, NOW(), NOW()),
(75.0, 9, 2, NOW(), NOW()), (80.0, 10, 2, NOW(), NOW()), (80.0, 11, 2, NOW(), NOW()), (75.0, 12, 2, NOW(), NOW()),
(70.0, 13, 2, NOW(), NOW()), (75.0, 14, 2, NOW(), NOW()), (75.0, 15, 2, NOW(), NOW()), (70.0, 16, 2, NOW(), NOW());

-- Profile 3: AIM 800 (stable_min = 85%)
INSERT INTO ability_evaluation_rules (stable_min, ability_id, target_profile_id, created_at, updated_at) VALUES
(85.0, 1, 3, NOW(), NOW()), (85.0, 2, 3, NOW(), NOW()), (85.0, 3, 3, NOW(), NOW()), (85.0, 4, 3, NOW(), NOW()),
(80.0, 5, 3, NOW(), NOW()), (85.0, 6, 3, NOW(), NOW()), (80.0, 7, 3, NOW(), NOW()), (85.0, 8, 3, NOW(), NOW()),
(85.0, 9, 3, NOW(), NOW()), (88.0, 10, 3, NOW(), NOW()), (88.0, 11, 3, NOW(), NOW()), (85.0, 12, 3, NOW(), NOW()),
(80.0, 13, 3, NOW(), NOW()), (85.0, 14, 3, NOW(), NOW()), (85.0, 15, 3, NOW(), NOW()), (80.0, 16, 3, NOW(), NOW());

-- Profile 4: AIM 990 (stable_min = 92%)
INSERT INTO ability_evaluation_rules (stable_min, ability_id, target_profile_id, created_at, updated_at) VALUES
(92.0, 1, 4, NOW(), NOW()), (92.0, 2, 4, NOW(), NOW()), (92.0, 3, 4, NOW(), NOW()), (92.0, 4, 4, NOW(), NOW()),
(90.0, 5, 4, NOW(), NOW()), (92.0, 6, 4, NOW(), NOW()), (90.0, 7, 4, NOW(), NOW()), (92.0, 8, 4, NOW(), NOW()),
(92.0, 9, 4, NOW(), NOW()), (95.0, 10, 4, NOW(), NOW()), (95.0, 11, 4, NOW(), NOW()), (92.0, 12, 4, NOW(), NOW()),
(90.0, 13, 4, NOW(), NOW()), (92.0, 14, 4, NOW(), NOW()), (92.0, 15, 4, NOW(), NOW()), (90.0, 16, 4, NOW(), NOW());

