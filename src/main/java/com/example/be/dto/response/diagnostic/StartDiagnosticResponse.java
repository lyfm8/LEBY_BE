package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Thông tin phiên làm bài trả về khi học viên bắt đầu bài kiểm tra chẩn đoán.
 */
@Getter
@Builder
public class StartDiagnosticResponse {
    /** ID lượt làm bài vừa tạo (FE giữ để gửi kèm khi nộp bài). */
    private Long attemptId;
    /** ID đề thi chẩn đoán đang thực hiện. */
    private Long diagnosticTestId;
    /** Thời điểm bắt đầu làm bài trên server (dùng đồng bộ đồng hồ đếm ngược). */
    private LocalDateTime startedAt;
    /** Thời lượng giới hạn của đề thi (tính bằng phút, đọc động từ DiagnosticTest). */
    private Integer durationMinutes;
}
