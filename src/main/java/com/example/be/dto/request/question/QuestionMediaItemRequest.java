package com.example.be.dto.request.question;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mục truyền thông (Media) được gán vào câu hỏi hoặc chùm câu hỏi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMediaItemRequest {

    @NotNull(message = "ID của media không được để trống")
    private Long mediaId;

    private Integer orderNo;
}

