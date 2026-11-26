package com.example.be.dto.request.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tiếp nhận yêu cầu tạo mới hoặc cập nhật mốc mục tiêu TOEIC (AIM) từ trang Quản trị.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetProfileRequest {

    // Ràng buộc điểm mục tiêu hợp lệ nằm trong thang điểm chuẩn TOEIC ETS (10 - 990)
    @NotNull(message = "Điểm mục tiêu AIM không được để trống")
    @Min(value = 10, message = "Điểm mục tiêu AIM tối thiểu là 10")
    @Max(value = 990, message = "Điểm mục tiêu AIM tối đa là 990")
    private Integer aimScore;

    // Giới hạn độ dài mô tả khớp với cột VARCHAR(500) trong bảng target_profiles
    @Size(max = 500, message = "Mô tả mục tiêu không được vượt quá 500 ký tự")
    private String description;
}
