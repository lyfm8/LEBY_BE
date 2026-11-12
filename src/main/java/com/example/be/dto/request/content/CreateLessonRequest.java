package com.example.be.dto.request.content;

import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    @Min(value = 1, message = "Thứ tự bài học phải lớn hơn hoặc bằng 1")
    private Integer orderNo;

    private EModuleStatus status;

    private String descriptions;

    @Builder.Default
    private ELessonType lessonType = ELessonType.VIDEO;

    @Min(value = 0, message = "Thời lượng bài học không được âm")
    @Max(value = 86400, message = "Thời lượng bài học tối đa là 24 giờ")
    @Builder.Default
    private Integer durationSeconds = 0;

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

