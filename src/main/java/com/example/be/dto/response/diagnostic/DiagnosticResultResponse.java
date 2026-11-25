package com.example.be.dto.response.diagnostic;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Kết quả tổng hợp trả về cho FE ngay sau khi học viên nộp bài kiểm tra chẩn đoán.
 */
@Getter
@Builder
public class DiagnosticResultResponse {
    /** ID lượt làm bài vừa nộp. */
    private Long attemptId;
    /** Tổng số câu trả lời đúng trên toàn bộ đề thi. */
    private Integer totalCorrect;
    /** Tổng số câu hỏi có trong đề thi (dùng làm mẫu số hiển thị tỷ lệ đúng). */
    private Integer totalQuestions;
    /** Danh sách điểm % và chỉ dẫn 4 nhánh Tầng 1 theo từng Part TOEIC. */
    private List<PartResultResponse> partResults;
    /** ID lộ trình học thích ứng (LearningPath) vừa được tự động sinh sau khi chấm. */
    private Long learningPathId;
}
