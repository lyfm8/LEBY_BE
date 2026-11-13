package com.example.be.dto.request.exam;

import com.example.be.enums.diagnostic.ETestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO yêu cầu tạo mới bộ đề thi (Diagnostic Test hoặc Mock Test).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExamRequest {

    @NotBlank(message = "Tên đề thi không được để trống")
    @Size(max = 150, message = "Tên đề thi không được vượt quá 150 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Loại đề thi bắt buộc phải chọn")
    private ETestType testType;

    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn hoặc bằng 1 phút")
    private Integer durationMinutes;

    /**
     * Danh sách ID câu hỏi được gán vào đề thi theo thứ tự.
     */
    @Builder.Default
    private List<Long> questionIds = new ArrayList<>();

    /**
     * Cờ đánh dấu lưu bản nháp:
     * - true: Lưu dưới dạng DRAFT (chưa hoàn thiện).
     * - false: Lưu đề thi hoàn tất (ở trạng thái ARCHIVE, sẵn sàng để Admin active sau).
     */
    @Builder.Default
    private Boolean isDraft = false;
}

