package com.example.be.dto.request.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO lưu trữ / cập nhật toàn bộ danh sách phụ đề của bài học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveLessonScriptsRequest {

    @NotNull(message = "Danh sách kịch bản không được null")
    @Valid
    private List<LessonScriptDto> scripts;
}

