package com.example.be.dto.response.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tối giản đại diện cho một Mô-đun học tập hiển thị công khai trên Landing Page.
 * Chỉ bao gồm tên mô-đun và số lượng bài giảng, bài thực hành, bài kiểm tra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCatalogModuleResponse {

    private Long id;

    private Integer sequence;

    private String title;

    private Integer videoLessonCount;

    private Integer practiceLessonCount;

    private Integer testCount;
}
