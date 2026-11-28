package com.example.be.service.content.service;

import com.example.be.dto.response.content.CourseCatalogResponse;

/**
 * Service Interface xử lý nghiệp vụ Thư viện khóa học 7 Part TOEIC cho học viên (UC17 Catalog).
 */
public interface UserCatalogService {

    /**
     * Tải toàn bộ cấu trúc Thư viện khóa học gồm 7 Phần thi TOEIC và danh sách Mô-đun đã xuất bản,
     * đối chiếu với lộ trình cá nhân hiện hành của học viên để phân loại quyền truy cập
     * ("Trọng tâm cần học", "Đã nắm vững • Học tự do", hoặc "Xem trước đề cương").
     *
     * @param userId ID định danh học viên đang đăng nhập (trích từ SecurityContext chống IDOR)
     * @return {@link CourseCatalogResponse} hoàn chỉnh cho giao diện Thư viện khóa học
     */
    CourseCatalogResponse getCourseCatalogForUser(Long userId);
}
