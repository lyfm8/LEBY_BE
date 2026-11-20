package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phân bố học viên theo từng mốc điểm mục tiêu TOEIC (AIM).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AimDistributionDto {
    private String aimName;
    private long count;
    private double percent;
}
