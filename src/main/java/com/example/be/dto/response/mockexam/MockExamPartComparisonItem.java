package com.example.be.dto.response.mockexam;

import com.example.be.dto.response.diagnostic.PartAbilityBreakdownResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Chi tiết kết quả một Part trong bài thi thử và đối sánh Trước vs Sau (UC18).
 */
@Getter
@Builder
public class MockExamPartComparisonItem {

    private Long partId;
    private Integer partNo;
    private String name;
    private Integer correctCount;
    private Integer totalQuestions;
    /** Tỷ lệ phần trăm làm đúng ở bài thi thử hiện tại (0..100). */
    private Integer score;
    /** Tỷ lệ phần trăm ở bài thi chẩn đoán đầu vào (chỉ có cho đề OUTPUT, null với PRACTICE). */
    private Integer beforeScore;
    /** Độ chênh lệch so với bài thi chẩn đoán đầu vào (score - beforeScore). */
    private Integer scoreDelta;
    /** Phân tích từng Ability con trong Part (chỉ có cho đề OUTPUT). */
    private List<PartAbilityBreakdownResponse> abilities;
}
