package com.example.be.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Yêu cầu chọn hoặc thay đổi mục tiêu điểm TOEIC (AIM) của học viên (UC05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectTargetRequest {

    /**
     * ID của mốc mục tiêu (TargetProfile) mà học viên lựa chọn.
     */
    @NotNull(message = "Vui lòng chọn một mốc mục tiêu điểm số trước khi tiếp tục")
    private Long targetProfileId;
}
