package com.example.be.dto.request.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Yêu cầu tạo mới câu hỏi đơn lập (Part 1, 2, 5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStandaloneQuestionRequest {

    @NotBlank(message = "Tên câu hỏi không được để trống")
    @Size(max = 200, message = "Tên câu hỏi tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Phần thi Part không được để trống")
    private Long partId;

    @NotEmpty(message = "Cần gán ít nhất một kỹ năng (Ability) cho câu hỏi")
    private List<Long> abilityIds;

    @NotNull(message = "Độ khó không được để trống")
    @Min(value = 1, message = "Độ khó tối thiểu là 1")
    @Max(value = 5, message = "Độ khó tối đa là 5")
    private Integer difficulty;

    /**
     * Nội dung đề bài câu hỏi (có thể để trống với câu hỏi Part 1/2 chỉ nghe audio).
     */
    private String text;

    /**
     * Danh sách các lựa chọn trả lời (3 lựa chọn cho Part 2, 4 lựa chọn cho Part 1 và 5).
     */
    @NotEmpty(message = "Danh sách phương án trả lời không được để trống")
    @Size(min = 3, max = 4, message = "Số lượng phương án trả lời phải từ 3 đến 4")
    private List<@Valid QuestionOptionRequest> options;

    /**
     * Nhãn của đáp án đúng duy nhất (A, B, C hoặc D).
     */
    @NotBlank(message = "Đáp án đúng không được để trống")
    @Pattern(regexp = "^[A-D]$", message = "Đáp án đúng phải là A, B, C hoặc D")
    private String correctKey;

    /**
     * Giải thích chi tiết đáp án kèm dịch nghĩa.
     */
    @Valid
    private QuestionExplanationRequest explanation;

    /**
     * Danh sách các tệp đa phương tiện đính kèm (ảnh tranh Part 1, file audio Part 1/2...).
     */
    private List<@Valid QuestionMediaItemRequest> medias;

    /**
     * Mô tả thêm hoặc ghi chú nội bộ.
     */
    private String descriptions;
}

