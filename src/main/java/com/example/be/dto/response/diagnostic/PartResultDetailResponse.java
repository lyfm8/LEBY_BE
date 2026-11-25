package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Chi tiết kết quả chẩn đoán của từng Part TOEIC (UC07).
 */
@Getter
@Builder
public class PartResultDetailResponse {
    /** ID phần thi Part. */
    private Long partId;
    /** Số thứ tự phần thi (1..7). */
    private Integer partNo;
    /** Tên phần thi (ví dụ: Part 1: Photographs). */
    private String name;
    /** Số câu trả lời đúng trong Part. */
    private Integer correctCount;
    /** Tổng số câu hỏi của Part trong đề thi. */
    private Integer totalQuestions;
    /** Điểm phần trăm đạt được (0..100). */
    private Integer score;
    /** Thang điểm phần trăm tối đa (100). */
    private Integer maxScore;
    /** Chỉ dẫn phân nhánh Tầng 1: PASS, CONFIRM, ABILITY_DIAGNOSTIC, FULL_PART. */
    private String directive;
    /** Nhãn tiếng Việt hiển thị cho trạng thái phân nhánh Tầng 1. */
    private String directiveLabel;
    /** Nhận xét sư phạm dành riêng cho Part này. */
    private String aiFeedback;
    /** Danh sách chi tiết các Năng lực con (Ability) thuộc Part này (UC07 Luồng 5a). */
    private List<PartAbilityBreakdownResponse> abilities;
}
