package com.example.be.dto.request.content;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một mục câu hỏi luyện tập được gắn vào bài học (Tab 3 - UC A05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionItemDto {

    /**
     * ID của câu hỏi trong Ngân hàng câu hỏi (có thể là câu đơn hoặc câu con, hoặc ID chùm cha để BE tự bóc tách).
     */
    @NotNull(message = "ID câu hỏi không được để trống")
    @Positive(message = "ID câu hỏi phải là số nguyên dương")
    private Long questionId;

    /**
     * Thứ tự hiển thị câu hỏi trong bài luyện tập (từ 1 đến 1000).
     */
    @NotNull(message = "Thứ tự câu hỏi không được để trống")
    @Min(value = 1, message = "Thứ tự câu hỏi phải từ 1 trở lên")
    @Max(value = 1000, message = "Thứ tự câu hỏi tối đa là 1000")
    private Integer orderNo;

    /**
     * Điểm số phân bổ cho câu hỏi trong bài luyện tập (từ 1 đến 100).
     */
    @NotNull(message = "Điểm số câu hỏi không được để trống")
    @Min(value = 1, message = "Điểm số tối thiểu là 1")
    @Max(value = 100, message = "Điểm số tối đa là 100")
    private Integer points;
}
