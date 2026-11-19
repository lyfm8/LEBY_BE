package com.example.be.enums.diagnostic;

/**
 * Trạng thái vòng đời của một bộ đề thi (Diagnostic Test hoặc Mock Test).
 * 
 * - DRAFT: Bản nháp, giáo viên/admin đang biên soạn, chưa công bố cho học viên.
 * - ACTIVE: Đề thi đang có hiệu lực.
 *           Đề chẩn đoán (DIAGNOSTIC): Chỉ duy nhất 1 đề được ACTIVE tại một thời điểm.
 *           Đề thi thử (FULL_MOCK_TEST): Có thể có nhiều đề ACTIVE đồng thời.
 * - ARCHIVE: Đề thi đã lưu kho/lưu trữ, học viên không thể bắt đầu làm bài mới.
 */
public enum EExamStatus {
    DRAFT,
    ACTIVE,
    ARCHIVE
}

