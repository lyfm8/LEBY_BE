-- =====================================================================================
-- V8: Bo sung phan loai tag de thi (PRACTICE / OUTPUT) cho bang diagnostic_tests
-- Phuc vu UC17 (Phong thi thu ETS 200 cau) & UC18 (Bao cao dau ra Truoc vs Sau)
-- =====================================================================================

ALTER TABLE diagnostic_tests
    ADD COLUMN exam_tag VARCHAR(20) NOT NULL DEFAULT 'PRACTICE';

ALTER TABLE diagnostic_tests
    ADD INDEX idx_diagnostic_tests_tag_status (exam_tag, exam_status);

-- Gan nhan OUTPUT cho de Full Mock Test mac dinh trong seed data de hoc vien co san de thi dau ra
UPDATE diagnostic_tests
SET exam_tag = 'OUTPUT'
WHERE id = 2
  AND test_type = 'FULL_MOCK_TEST';
