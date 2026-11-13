-- =====================================================================================
-- Migration V3: Cập nhật trạng thái vòng đời đề thi (EExamStatus) và thông tin Audit
-- =====================================================================================

-- 1. Thêm cột exam_status (mặc định là DRAFT) và updated_by
ALTER TABLE diagnostic_tests 
    ADD COLUMN exam_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN updated_by VARCHAR(100);

-- 2. Chuyển đổi dữ liệu cũ (nếu cột status đang tồn tại)
UPDATE diagnostic_tests 
SET exam_status = CASE 
    WHEN status = 1 OR status = true THEN 'ACTIVE' 
    ELSE 'ARCHIVE' 
END,
updated_by = 'Admin';

-- 3. Xóa cột status boolean cũ
ALTER TABLE diagnostic_tests DROP COLUMN status;

