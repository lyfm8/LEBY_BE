package com.example.be.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lựa chọn phương án trả lời trong câu hỏi (A, B, C, D).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionRequest {

    @NotBlank(message = "Nhãn phương án (A, B, C, D) không được để trống")
    @Pattern(regexp = "^[A-D]$", message = "Nhãn phương án chỉ chấp nhận A, B, C hoặc D")
    private String key;

    private String text;
}

