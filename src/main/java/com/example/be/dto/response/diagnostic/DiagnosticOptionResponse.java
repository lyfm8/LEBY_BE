package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

/**
 * Phương án trả lời trắc nghiệm của một câu hỏi trong phòng thi chẩn đoán.
 */
@Getter
@Builder
public class DiagnosticOptionResponse {
    /** Ký hiệu phương án (A, B, C, D). */
    private String key;
    /** Nội dung văn bản của phương án trả lời. */
    private String text;
}
