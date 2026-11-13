package com.example.be.dto.request.exam;

import com.example.be.enums.diagnostic.ETestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO yêu cầu tự động chọn câu hỏi ngẫu nhiên từ Question Bank theo ma trận.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoSelectQuestionsRequest {

    @NotNull(message = "Loại đề thi bắt buộc phải chọn")
    private ETestType testType;

    /**
     * Tổng số câu hỏi mong muốn (áp dụng cho DIAGNOSTIC, mặc định 60 nếu null).
     */
    private Integer totalQuestions;

    /**
     * Cấu hình phân bổ số câu hỏi theo từng Part (key là partNumber từ 1 đến 7, value là số lượng câu).
     * Nếu null, hệ thống sẽ sử dụng ma trận chuẩn mặc định:
     * - FULL_MOCK_TEST: Part 1=6, Part 2=25, Part 3=39, Part 4=30, Part 5=30, Part 6=16, Part 7=54 (Tổng 200 câu).
     * - DIAGNOSTIC: 60 câu phân bổ đều và bắt buộc bao phủ tất cả Ability trong database.
     */
    private Map<Integer, Integer> customDistribution;
}

