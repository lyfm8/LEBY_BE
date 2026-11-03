package com.example.be.dto.request.content;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO yêu cầu sắp xếp lại thứ tự sư phạm của danh sách Module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderModulesRequest {

    @NotEmpty(message = "Danh sách ID Module không được rỗng")
    private List<Long> orderedModuleIds;
}

