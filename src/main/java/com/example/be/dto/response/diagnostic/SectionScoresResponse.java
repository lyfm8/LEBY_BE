package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

/**
 * Thống kê điểm quy đổi và tỷ lệ đúng theo hai kỹ năng Listening & Reading (UC07).
 */
@Getter
@Builder
public class SectionScoresResponse {
    /** Điểm quy đổi kỹ năng Listening trên thang 495 (chia hết cho 5). */
    private Integer listening;
    /** Điểm quy đổi kỹ năng Reading trên thang 495 (chia hết cho 5). */
    private Integer reading;
    /** Số câu trả lời đúng phần Listening. */
    private Integer listeningCorrect;
    /** Tổng số câu hỏi phần Listening trong đề. */
    private Integer listeningTotal;
    /** Số câu trả lời đúng phần Reading. */
    private Integer readingCorrect;
    /** Tổng số câu hỏi phần Reading trong đề. */
    private Integer readingTotal;
    /** Tỷ lệ phần trăm trả lời đúng phần Listening (0..100). */
    private Integer listeningPercent;
    /** Tỷ lệ phần trăm trả lời đúng phần Reading (0..100). */
    private Integer readingPercent;
}
