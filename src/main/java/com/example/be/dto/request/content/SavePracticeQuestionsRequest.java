package com.example.be.dto.request.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu lưu/đồng bộ toàn bộ danh sách câu hỏi luyện tập (Practice Questions) của một bài học.
 * Áp dụng giới hạn @Size(max = 100) để chống tấn công JSON Payload DoS theo quy chuẩn bảo mật.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavePracticeQuestionsRequest {

    /**
     * Danh sách câu hỏi luyện tập theo thứ tự mong muốn (cho phép rỗng khi giáo viên gỡ hết câu hỏi).
     */
    @NotNull(message = "Danh sách câu hỏi luyện tập không được null")
    @Size(max = 100, message = "Mỗi bài học chỉ được gắn tối đa 100 câu hỏi luyện tập")
    @Valid
    private List<PracticeQuestionItemDto> items;
}
