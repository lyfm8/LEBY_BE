package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả điểm số và chỉ dẫn Tầng 1 của từng Part TOEIC sau khi chấm bài chẩn đoán.
 */
@Getter
@Builder
public class PartResultResponse {
    /** ID của Part (Part 1 - Part 7). */
    private Long partId;
    /** Tên hiển thị của Part (lấy từ Part.name). */
    private String partName;
    /** Tỷ lệ trả lời đúng của Part trên tổng số câu của Part trong đề (thang 0 - 100%). */
    private Float score;
    /** Nhánh chỉ dẫn Tầng 1: PASS, CONFIRM, ABILITY_DIAGNOSTIC hoặc FULL_PART. */
    private String directive;
}
