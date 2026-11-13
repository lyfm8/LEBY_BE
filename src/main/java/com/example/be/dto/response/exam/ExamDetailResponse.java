package com.example.be.dto.response.exam;

import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO trả về chi tiết toàn diện của một bộ đề thi (gồm cả danh sách câu hỏi và ma trận).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {

    private Long id;
    private String title;
    private String description;
    private ETestType testType;
    private EExamStatus examStatus;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private Integer listeningCount;
    private Integer readingCount;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Phân bổ số câu theo từng Part (key là partNumber 1..7).
     */
    @Builder.Default
    private Map<Integer, Integer> partDistribution = new HashMap<>();

    /**
     * Danh sách câu hỏi trong đề được sắp xếp theo orderNo.
     */
    @Builder.Default
    private List<ExamQuestionItemResponse> questions = new ArrayList<>();
}

