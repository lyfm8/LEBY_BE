package com.example.be.dto.request.diagnostic;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu bắt đầu bài kiểm tra chẩn đoán.
 * Cho phép để trống {@code diagnosticTestId} và {@code targetProfileId} để hệ thống tự động
 * sử dụng đề thi DIAGNOSTIC đang ACTIVE và mục tiêu AIM hiện tại của học viên.
 */
@Getter
@Setter
@NoArgsConstructor
public class StartDiagnosticRequest {

    /** ID đề thi chẩn đoán muốn bắt đầu (tùy chọn; mặc định lấy đề DIAGNOSTIC đang ACTIVE). */
    private Long diagnosticTestId;

    /** ID mục tiêu AIM của học viên tại thời điểm bắt đầu làm bài (phục vụ kiểm tra BR02). */
    private Long targetProfileId;
}
