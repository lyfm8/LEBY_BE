package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đại diện cho một Phần thi TOEIC (Part 1 đến Part 7) hiển thị công khai trên Landing Page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCatalogPartResponse {

    private Long partId;

    private String partName;

    private ESection section;

    private List<PublicCatalogModuleResponse> modules;
}
