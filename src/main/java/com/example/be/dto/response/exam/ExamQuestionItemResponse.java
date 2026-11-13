package com.example.be.dto.response.exam;

import com.example.be.enums.ability.ESection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO đại diện cho một câu hỏi nằm trong Đề thi hoặc được bốc ra từ Question Bank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionItemResponse {

    private Long id;
    private Long questionId;
    private Integer orderNo;
    private Integer points;
    private String questionCode;
    private String questionName;
    private Integer partId;
    private Integer partNumber;
    private String partName;
    private ESection section;
    private Integer difficulty;

    @Builder.Default
    private List<String> abilityNames = new ArrayList<>();
}

