package com.example.be.dto.response.content;

import com.example.be.enums.content.EModuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chi tiết đầy đủ của Module phục vụ màn hình biên tập Module ở vùng Editor 75% bên phải.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDetailResponse {

    private Long id;
    private String title;
    private String type;
    private Integer sequence;
    private EModuleStatus status;
    private Integer defaultPassScore;
    private Integer requiredCompletionRate;
    private String descriptions;
    private Long partId;
    private String partName;
    private List<Long> abilityIds;
    private List<String> abilityNames;
    private int totalLessons;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

