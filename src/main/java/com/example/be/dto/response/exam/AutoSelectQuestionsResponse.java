package com.example.be.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả về kết quả tự động chọn câu hỏi ngẫu nhiên từ Question Bank theo ma trận.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoSelectQuestionsResponse {

    private Integer totalSelected;

    @Builder.Default
    private List<Long> questionIds = new ArrayList<>();

    @Builder.Default
    private List<ExamQuestionItemResponse> questions = new ArrayList<>();
}

