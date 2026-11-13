package com.example.be.dto.response.exam;

import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO hiển thị từng dòng trong bảng Danh sách đề thi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamListItemResponse {

    private Long id;
    private String title;
    private String description;
    private ETestType testType;
    private EExamStatus examStatus;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private LocalDateTime updatedAt;
    private String updatedBy;
}

