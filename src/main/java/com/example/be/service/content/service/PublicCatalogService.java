package com.example.be.service.content.service;

import com.example.be.dto.response.content.PublicCatalogResponse;

/**
 * Service Interface cung cấp dữ liệu chương trình học 7 Part TOEIC công khai cho Landing Page.
 */
public interface PublicCatalogService {

    /**
     * Lấy toàn bộ cấu trúc 7 Part cùng các mô-đun đã xuất bản và thống kê bài học/test.
     *
     * @return {@link PublicCatalogResponse}
     */
    PublicCatalogResponse getPublicCatalog();
}
