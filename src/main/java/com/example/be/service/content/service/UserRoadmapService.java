package com.example.be.service.content.service;

import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;

/**
 * Service Interface xử lý nghiệp vụ Lộ trình học cá nhân (UC09) và Chi tiết Module (UC10) cho học viên.
 */
public interface UserRoadmapService {

    /**
     * Lấy lộ trình học tập hiện hành (ACTIVE hoặc COMPLETED mới nhất) của học viên đang đăng nhập.
     * Trả về {@code null} nếu học viên chưa chọn mục tiêu hoặc chưa hoàn thành bài thi chẩn đoán.
     *
     * @param userId ID định danh của học viên (lấy từ SecurityContext để chống IDOR)
     * @return {@link RoadmapResponse} chứa thông tin lộ trình và danh sách chặng học, hoặc {@code null}
     */
    RoadmapResponse getCurrentUserRoadmap(Long userId);

    /**
     * Lấy thông tin chi tiết một Module thuộc lộ trình học hiện hành của học viên kèm danh sách bài học
     * và thông số cấu hình động của bài thi vượt ải.
     *
     * @param userId   ID định danh của học viên đang đăng nhập
     * @param moduleId ID của Module cần xem chi tiết
     * @return {@link UserModuleDetailResponse} chứa đầy đủ bài giảng, bài tập thực hành và điều kiện vượt ải
     */
    UserModuleDetailResponse getModuleDetailForUser(Long userId, Long moduleId);
}
