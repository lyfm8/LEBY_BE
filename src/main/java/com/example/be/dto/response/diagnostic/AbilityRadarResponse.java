package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

/**
 * Điểm một chiều trên Biểu đồ Radar 7 chiều theo từng Part TOEIC (UC07).
 */
@Getter
@Builder
public class AbilityRadarResponse {
    /** Số thứ tự Part (1..7). */
    private Integer partNo;
    /** Mã định danh trục radar (ví dụ: PART_1). */
    private String code;
    /** Tên hiển thị của chiều năng lực trên biểu đồ. */
    private String name;
    /** Điểm phần trăm đạt được của chiều này (0..100). */
    private Integer score;
    /** Trạng thái đánh giá: STABLE, DEVELOPING, WEAK hoặc UNCERTAIN. */
    private String status;
}
