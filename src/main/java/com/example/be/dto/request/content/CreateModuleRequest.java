package com.example.be.dto.request.content;

import com.example.be.enums.content.EModuleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu tạo mới Module học tập (A05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateModuleRequest {

    @NotBlank(message = "Tên Module không được để trống")
    @Size(max = 200, message = "Tên Module tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Loại Module không được để trống")
    @Size(max = 100, message = "Loại Module tối đa 100 ký tự")
    private String type;

    @Min(value = 1, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 1")
    private Integer sequence;

    private EModuleStatus status;

    @Min(value = 50, message = "Điểm đỗ mặc định tối thiểu là 50")
    @Max(value = 100, message = "Điểm đỗ mặc định tối đa là 100")
    private Integer defaultPassScore;

    @Min(value = 50, message = "Tỷ lệ hoàn thành tối thiểu là 50%")
    @Max(value = 100, message = "Tỷ lệ hoàn thành tối đa là 100%")
    private Integer requiredCompletionRate;

    private Long partId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một năng lực (Ability) mục tiêu gắn kèm")
    private List<Long> abilityIds;

    private String descriptions;
}

