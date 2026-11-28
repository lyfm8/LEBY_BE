package com.example.be.dto.response.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp cấu trúc chương trình 7 Part TOEIC công khai cho Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCatalogResponse {

    private Integer totalParts;

    private Integer totalModules;

    private Integer totalLessons;

    private List<PublicCatalogPartResponse> parts;
}
