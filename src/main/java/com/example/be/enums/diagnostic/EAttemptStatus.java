package com.example.be.enums.diagnostic;

/**
 * Trạng thái lượt thi chẩn đoán và thi thử.
 */
public enum EAttemptStatus {
    IN_PROGRESS,   // Đang trong thời gian làm bài
    SUBMITTED,     // Học viên chủ động bấm nộp bài
    TIMED_OUT,     // Hết giờ làm bài, máy chủ tự động thu bài
    ABANDONED      // Bỏ thi giữa chừng
}
