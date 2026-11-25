package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Tóm tắt khuyến nghị học tập và lộ trình cá nhân hóa theo mục tiêu AIM (UC07).
 */
@Getter
@Builder
public class DiagnosticAiRoadmapSummaryResponse {
    /** Nhận định tổng quan dựa trên phân tích Radar 7 chiều. */
    private String radarInsight;
    /** Danh sách số thứ tự các Part cần ưu tiên cải thiện. */
    private List<Integer> focusParts;
    /** Số lượng mô-đun trong lộ trình cá nhân hóa đã sinh. */
    private Integer moduleCount;
    /** Nhãn nút hành động chính (chuyển sang UC09). */
    private String recommendedAction;
    /** Nhãn nút hành động phụ. */
    private String secondaryAction;
}
