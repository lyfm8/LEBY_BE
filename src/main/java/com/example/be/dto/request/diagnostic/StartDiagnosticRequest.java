package com.example.be.dto.request.diagnostic;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu bắt đầu bài kiểm tra chẩn đoán.
 */
@Getter
@Setter
@NoArgsConstructor
public class StartDiagnosticRequest {

    /** ID đề thi chẩn đoán muốn bắt đầu (loại DIAGNOSTIC). */
    @NotNull(message = "diagnosticTestId không được để trống")
    private Long diagnosticTestId;
}
