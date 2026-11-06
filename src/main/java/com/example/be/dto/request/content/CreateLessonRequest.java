package com.example.be.dto.request.content;

import com.example.be.enums.content.EModuleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu tạo mới bài học (Lesson) trong một Module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonRequest {

    @NotBlank(message = "Tên bài học không được để trống")
    @Size(max = 200, message = "Tên bài học tối đa 200 ký tự")
    private String title;

    @NotNull(message = "Thứ tự bài học không được để trống")
    @Min(value = 1, message = "Thứ tự bài học phải lớn hơn hoặc bằng 1")
    private Integer orderNo;

    @NotNull(message = "Trạng thái bài học không được để trống")
    private EModuleStatus status;

    private String descriptions;

    @Size(max = 500, message = "Đường dẫn file âm thanh tối đa 500 ký tự")
    private String audioUrl;

    @Size(max = 500, message = "Đường dẫn video tối đa 500 ký tự")
    private String videoUrl;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String imageUrl;

    @Builder.Default
    private Boolean isFree = true;

    private Long abilityId;
}

