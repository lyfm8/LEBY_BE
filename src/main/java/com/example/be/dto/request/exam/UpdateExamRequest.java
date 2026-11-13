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
 * DTO yêu cầu cập nhật thông tin và danh sách câu hỏi của đề thi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExamRequest {

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
     * Danh sách ID câu hỏi cập nhật cho đề thi.
     */
    @Builder.Default
    private List<Long> questionIds = new ArrayList<>();

    /**
     * Cờ đánh dấu lưu bản nháp:
     * - true: Giữ/Chuyển trạng thái DRAFT.
     * - false: Chuyển sang ARCHIVE nếu đang là DRAFT (hoặc giữ nguyên nếu đã ACTIVE/ARCHIVE).
     */
    @Builder.Default
    private Boolean isDraft = false;
}

