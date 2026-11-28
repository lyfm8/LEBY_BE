package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.content.PublicCatalogResponse;
import com.example.be.service.content.service.PublicCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API công khai hiển thị danh mục chương trình 7 Part TOEIC trên Landing Page.
 */
@RestController
@RequestMapping("/api/public/catalog")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    /**
     * Lấy danh sách 7 Part cùng các mô-đun đã xuất bản và số lượng bài giảng/thực hành/test.
     *
     * @return {@link ApiResponse} chứa {@link PublicCatalogResponse}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PublicCatalogResponse>> getPublicCatalog() {
        PublicCatalogResponse catalog = publicCatalogService.getPublicCatalog();
        return ResponseEntity.ok(ApiResponse.<PublicCatalogResponse>builder()
                .success(true)
                .message("Tải danh mục chương trình học thành công")
                .data(catalog)
                .build());
    }
}
