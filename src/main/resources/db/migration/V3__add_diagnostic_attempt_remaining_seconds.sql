-- Bo sung cot luu so giay con lai tren luot thi DiagnosticAttempt
-- Giu dong ho ngung dong khi hoc vien tam dung, crash trinh duyet hoac thoat giua chung (UC06 Luong 4b & 5a)
ALTER TABLE diagnostic_attempts
    ADD COLUMN remaining_seconds INT NULL;
