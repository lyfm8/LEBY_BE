package com.example.be.dto.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Integer orderNo;

    @NotBlank(message = "Thời điểm bắt đầu không được để trống")
    @Size(max = 20, message = "Thời điểm bắt đầu tối đa 20 ký tự")
    private String startTime;

    @NotBlank(message = "Thời điểm kết thúc không được để trống")
    @Size(max = 20, message = "Thời điểm kết thúc tối đa 20 ký tự")
    private String endTime;

    @Size(max = 100, message = "Tên người nói tối đa 100 ký tự")
    private String speaker;

    @NotBlank(message = "Nội dung tiếng Anh không được để trống")
    private String contentEn;

    private String contentVi;
}

