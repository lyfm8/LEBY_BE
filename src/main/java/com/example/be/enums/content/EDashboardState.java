package com.example.be.enums.content;

/**
 * Các trạng thái vòng đời học tập hiển thị trên Dashboard cá nhân của học viên (UC08).
 * Giúp Frontend phân luồng chính xác theo từng giai đoạn: chọn AIM, thi chẩn đoán, đang học hay đã hoàn thành.
 */
public enum EDashboardState {
    /** Học viên mới chưa thiết lập mốc điểm TOEIC mục tiêu AIM (UC08 Luồng thay thế 2a). */
    NEED_TARGET,

    /** Học viên đã chọn AIM nhưng chưa hoàn thành bài thi chẩn đoán đầu vào (UC08 Luồng thay thế 2b). */
    NEED_DIAGNOSTIC,

    /** Học viên đã nộp bài chẩn đoán nhưng lộ trình học chưa được đồng bộ hoàn chỉnh (UC08 Ngoại lệ 4b). */
    SYNCING_ROADMAP,

    /** Học viên đang theo học lộ trình thích ứng cá nhân hóa hiện hành (UC08 Luồng chính). */
    IN_PROGRESS,

    /** Học viên đã hoàn thành 100% mô-đun lộ trình hoặc PASS 100% 7 Part từ chẩn đoán (UC08 Luồng thay thế 4a). */
    COMPLETED
}
