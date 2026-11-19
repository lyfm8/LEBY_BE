package com.example.be.dto.request.setting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO gửi yêu cầu lưu ma trận ngưỡng điểm Part theo AIM (A08).
 * 3 mức đánh giá (Weak / Confirm / Pass) tương ứng 2 mốc điểm (confirmingScore / passScore).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavePartThresholdsBatchRequest {

    @NotNull(message = "ID mục tiêu AIM không được để trống")
    private Long targetProfileId;

    @Valid
    @NotNull(message = "Danh sách ngưỡng Part không được để trống")
    @Size(min = 1, max = 7, message = "Danh sách ngưỡng phải có từ 1 đến 7 Part")
    private List<PartThresholdItem> thresholds;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartThresholdItem {

        @NotNull(message = "ID Part không được để trống")
        private Long partId;

        @NotNull(message = "Ngưỡng Đạt chuẩn (Pass) không được để trống")
        @Min(value = 0, message = "Ngưỡng Đạt chuẩn tối thiểu là 0%")
        @Max(value = 100, message = "Ngưỡng Đạt chuẩn tối đa là 100%")
        private Float passScore;

        @NotNull(message = "Ngưỡng Cận chuẩn (Confirm) không được để trống")
        @Min(value = 0, message = "Ngưỡng Cận chuẩn tối thiểu là 0%")
        @Max(value = 100, message = "Ngưỡng Cận chuẩn tối đa là 100%")
        private Float confirmingScore;
    }
}

