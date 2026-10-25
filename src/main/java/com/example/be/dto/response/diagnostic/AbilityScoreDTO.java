package com.example.be.dto.response.diagnostic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chứa điểm số và độ chính xác của từng Năng lực (Ability) sau bài thi chẩn đoán.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityScoreDTO {

    private Long abilityId;
    private String abilityName;
    private float accuracyRate;      // Tỷ lệ đúng 0.0 - 100.0%
    private int correctCount;        // Số câu đúng
    private int totalEvidenceCount;  // Tổng số câu hỏi bằng chứng
}
