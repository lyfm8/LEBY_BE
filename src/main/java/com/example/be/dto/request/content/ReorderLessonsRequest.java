package com.example.be.dto.request.content;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu sắp xếp lại thứ tự các bài học trong một Module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderLessonsRequest {

    @NotEmpty(message = "Danh sách ID bài học không được rỗng")
    private List<Long> orderedLessonIds;
}

