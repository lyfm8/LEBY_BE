package com.example.be.dto.request.content;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho một bản ghi phụ đề / kịch bản lời thoại của bài nghe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonScriptDto {

    private Long id;

    @NotNull(message = "Thứ tự câu thoại không được để trống")
    @Min(value = 1, message = "Thứ tự câu thoại phải lớn hơn hoặc bằng 1")
    private Integer orderNo;

    @NotBlank(message = "Thời điểm bắt đầu không được để trống")
    @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$", message = "Định dạng thời điểm bắt đầu phải là mm:ss hoặc hh:mm:ss")
    @Size(max = 20, message = "Thời điểm bắt đầu tối đa 20 ký tự")
    private String startTime;

    @NotBlank(message = "Thời điểm kết thúc không được để trống")
    @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$", message = "Định dạng thời điểm kết thúc phải là mm:ss hoặc hh:mm:ss")
    @Size(max = 20, message = "Thời điểm kết thúc tối đa 20 ký tự")
    private String endTime;

    @Size(max = 100, message = "Tên người nói tối đa 100 ký tự")
    private String speaker;

    @NotBlank(message = "Nội dung tiếng Anh không được để trống")
    @Size(max = 5000, message = "Nội dung tiếng Anh tối đa 5000 ký tự")
    private String contentEn;

    @Size(max = 5000, message = "Nội dung dịch tiếng Việt tối đa 5000 ký tự")
    private String contentVi;
}

