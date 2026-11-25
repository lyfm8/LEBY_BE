package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

/**
 * Chi tiết năng lực con (Ability) thuộc một Part hoặc tổng hợp trong báo cáo UC07.
 */
@Getter
@Builder
public class PartAbilityBreakdownResponse {
    /** ID định danh của Ability. */
    private Long abilityId;
    /** Tên kỹ năng năng lực (tiếng Việt/chuyên môn). */
    private String abilityName;
    /** Số thứ tự Part chứa Ability này (1..7). */
    private Integer partNo;
    /** Tên Part chứa Ability. */
    private String partName;
    /** Số câu trả lời đúng thuộc Ability này. */
    private Integer correctCount;
    /** Tổng số câu hỏi đánh giá Ability này trong đề. */
    private Integer totalCount;
    /** Tỷ lệ trả lời đúng (0..100). */
    private Integer accuracyPercent;
    /** Trạng thái phân loại (WEAK, DEVELOPING, STABLE, UNCERTAIN). */
    private String status;
}
