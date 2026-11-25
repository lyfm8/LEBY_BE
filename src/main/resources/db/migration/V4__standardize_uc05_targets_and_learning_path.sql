-- =====================================================================================
-- Migration V4: Chuẩn hóa dữ liệu UC05 (Chọn / Đổi mục tiêu AIM)
-- Tương thích MySQL 8.0+
-- =====================================================================================

-- 1. Xóa unique constraint/index trên learning_paths.diagnostic_attempt_id nếu tồn tại
--    để hỗ trợ BR02 (nhiều lộ trình tái sử dụng cùng 1 bài chẩn đoán khi đổi AIM)
SET @uk_diag_index = (
    SELECT INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'learning_paths'
      AND COLUMN_NAME = 'diagnostic_attempt_id'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);

SET @drop_uk_sql = IF(
    @uk_diag_index IS NOT NULL,
    CONCAT('ALTER TABLE learning_paths DROP INDEX `', @uk_diag_index, '`'),
    'SELECT 1'
);

PREPARE stmt_drop_uk FROM @drop_uk_sql;
EXECUTE stmt_drop_uk;
DEALLOCATE PREPARE stmt_drop_uk;

-- 2. Bảo đảm tồn tại mốc AIM 850 chuẩn hóa theo UC05 (450, 550, 650, 750, 850)
INSERT INTO target_profiles (target_total_score, status, description, created_at, updated_at)
SELECT 850, 1, 'Làm chủ hoàn toàn kỹ năng nghe đọc học thuật và giao tiếp chuyên sâu.', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM target_profiles WHERE target_total_score = 850
);

SET @profile_850_id = (
    SELECT id FROM target_profiles WHERE target_total_score = 850 ORDER BY id ASC LIMIT 1
);

-- 3. Nạp ngưỡng 7 Part cho mốc AIM 850 nếu chưa cấu hình
INSERT INTO target_part_thresholds (pass_score, confirming_score, full_part_threshold, target_profile_id, part_id, created_at, updated_at)
SELECT
    CASE p.id
        WHEN 1 THEN 92.0
        WHEN 2 THEN 88.0
        WHEN 3 THEN 85.0
        WHEN 4 THEN 85.0
        WHEN 5 THEN 88.0
        WHEN 6 THEN 85.0
        WHEN 7 THEN 82.0
        ELSE 85.0
    END,
    CASE p.id
        WHEN 1 THEN 80.0
        WHEN 2 THEN 75.0
        WHEN 3 THEN 70.0
        WHEN 4 THEN 70.0
        WHEN 5 THEN 75.0
        WHEN 6 THEN 70.0
        WHEN 7 THEN 68.0
        ELSE 72.0
    END,
    CASE p.id
        WHEN 1 THEN 55.0
        WHEN 2 THEN 50.0
        WHEN 3 THEN 45.0
        WHEN 4 THEN 45.0
        WHEN 5 THEN 50.0
        WHEN 6 THEN 45.0
        WHEN 7 THEN 42.0
        ELSE 48.0
    END,
    @profile_850_id,
    p.id,
    NOW(),
    NOW()
FROM parts p
WHERE @profile_850_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM target_part_thresholds t
      WHERE t.target_profile_id = @profile_850_id AND t.part_id = p.id
  );

-- 4. Nạp ngưỡng Module cho mốc AIM 850 nếu chưa cấu hình (tăng nhẹ theo default_pass_score, tối đa 95)
INSERT INTO module_target_thresholds (pass_score, module_id, target_profile_id, created_at, updated_at)
SELECT
    LEAST(95, m.default_pass_score + 15),
    m.id,
    @profile_850_id,
    NOW(),
    NOW()
FROM modules m
WHERE @profile_850_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM module_target_thresholds mtt
      WHERE mtt.target_profile_id = @profile_850_id AND mtt.module_id = m.id
  );

-- 5. Chuyển tham chiếu user và learning_path đang trỏ vào mốc lệch chuẩn (800, 990) về mốc 850
UPDATE users u
JOIN target_profiles tp ON u.target_profile_id = tp.id
SET u.target_profile_id = @profile_850_id
WHERE tp.target_total_score IN (800, 990)
  AND @profile_850_id IS NOT NULL;

UPDATE learning_paths lp
JOIN target_profiles tp ON lp.target_profile_id = tp.id
SET lp.target_profile_id = @profile_850_id
WHERE tp.target_total_score IN (800, 990)
  AND @profile_850_id IS NOT NULL;

-- 6. Xóa dữ liệu ngưỡng và bản ghi 2 mốc 800, 990 không thuộc 5 mốc chuẩn UC05
DELETE tpt FROM target_part_thresholds tpt
JOIN target_profiles tp ON tpt.target_profile_id = tp.id
WHERE tp.target_total_score IN (800, 990);

DELETE mtt FROM module_target_thresholds mtt
JOIN target_profiles tp ON mtt.target_profile_id = tp.id
WHERE tp.target_total_score IN (800, 990);

DELETE FROM target_profiles
WHERE target_total_score IN (800, 990);
