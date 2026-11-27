package com.example.be.dto.request.content;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho thao tác tự động bốc ngẫu nhiên câu hỏi từ kho câu hỏi của Ability (UC A07 - Luồng 6a).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoSelectModuleTestQuestionsRequest {

    /**
     * Số lượng câu hỏi cần bốc ngẫu nhiên từ ngân hàng câu hỏi.
     */
    @NotNull(message = "Số lượng câu hỏi cần chọn không được để trống")
    @Min(value = 1, message = "Số lượng câu hỏi và thời gian làm bài phải lớn hơn 0")
    @Max(value = 100, message = "Số lượng câu hỏi tối đa là 100 câu")
    private Integer questionCount;

    /**
     * Chỉ lấy câu hỏi khớp Ability của mô-đun (mặc định true để đảm bảo chuẩn sư phạm).
     */
    private Boolean onlyMatchingAbilities;
}
