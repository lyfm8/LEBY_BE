package com.example.be.dto.request.question;

import jakarta.validation.Valid;
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
 * Yêu cầu cập nhật chùm câu hỏi chung ngữ cảnh (Part 3, 4, 6, 7).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionGroupRequest {

    @NotBlank(message = "Tên chùm câu hỏi không được để trống")
    @Size(max = 200, message = "Tên chùm câu hỏi tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Phần thi Part không được để trống")
    private Long partId;

    private String instruction;
    private String passage;
    private String transcript;
    private String translation;
    private List<@Valid QuestionMediaItemRequest> medias;

    @NotEmpty(message = "Chùm câu hỏi phải có ít nhất 2 câu hỏi con")
    @Size(min = 2, max = 5, message = "Số lượng câu hỏi con trong chùm phải từ 2 đến 5 câu")
    private List<@Valid QuestionGroupItemRequest> items;

    private String descriptions;
}

