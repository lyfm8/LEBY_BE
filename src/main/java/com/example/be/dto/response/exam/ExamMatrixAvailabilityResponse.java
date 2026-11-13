package com.example.be.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO trả về số lượng câu hỏi khả dụng hiện có trong Question Bank theo từng Part và Ability.
 * Phục vụ bảng ma trận (cột "Hiện có") và checklist đánh giá độ bao phủ Ability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixAvailabilityResponse {

    /**
     * Số câu hỏi hiện có theo Part (key là partNumber 1..7, value là tổng số câu hỏi đơn lập hoặc con trong chùm).
     */
    @Builder.Default
    private Map<Integer, Integer> partAvailableCounts = new HashMap<>();

    /**
     * Số câu hỏi đọc đơn (Single Passages) trong Part 7.
     */
    @Builder.Default
    private Integer singlePassageCount = 0;

    /**
     * Số câu hỏi đọc đôi/ba (Multiple Passages) trong Part 7.
     */
    @Builder.Default
    private Integer multiplePassageCount = 0;

    /**
     * Danh sách toàn bộ Ability trong database kèm số câu hỏi hiện có.
     */
    @Builder.Default
    private List<AbilityAvailabilityItem> abilities = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityAvailabilityItem {
        private Long id;
        private String name;
        private String section;
        private Integer partNumber;
        private Integer availableQuestionCount;
    }
}

