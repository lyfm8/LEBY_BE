package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đại diện cho một Phần thi TOEIC (Part 1 đến Part 7) trong Thư viện khóa học (UC17 Catalog),
 * bao gồm thống kê tổng hợp và danh sách các Mô-đun đã xuất bản thuộc phần thi đó.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogPartGroupResponse {

    private Long partId;

    private String partName;

    private String partDescription;

    private ESection section;

    private Integer totalModules;

    private Integer roadmapModulesCount;

    private Integer completedModulesCount;

    private Integer freeStudyModulesCount;

    private List<CatalogModuleItemResponse> modules;
}
