package com.example.be.service.user.service;

import com.example.be.dto.response.user.SelectTargetResponse;
import com.example.be.dto.response.user.UserTargetProfileResponse;

import java.util.List;

/**
 * Service xử lý nghiệp vụ chọn và thay đổi mục tiêu điểm TOEIC (AIM) của học viên (UC05).
 */
public interface UserTargetService {

    /**
     * Lấy danh sách toàn bộ các mốc mục tiêu AIM đang được kích hoạt cho học viên lựa chọn.
     *
     * @return Danh sách {@link UserTargetProfileResponse} sắp xếp theo điểm tăng dần
     */
    List<UserTargetProfileResponse> getAllActiveTargets();

    /**
     * Xử lý xác nhận chọn lần đầu hoặc thay đổi mục tiêu AIM hiện tại của học viên.
     * - Lần đầu: Cập nhật mục tiêu và trả về chỉ dẫn đi tới bài thi chẩn đoán.
     * - Đổi AIM (đã có dữ liệu chẩn đoán SUBMITTED): Lưu trữ lộ trình cũ, tái dùng bài làm chẩn đoán
     *   để phân tầng lại theo ngưỡng AIM mới (BR02) và bảo lưu tiến độ bài học / module đỗ (BR06).
     * - Đổi AIM (chưa có dữ liệu chẩn đoán hợp lệ): Cập nhật mục tiêu mới và điều hướng thi chẩn đoán (Luồng 7b).
     *
     * @param userId          ID học viên đang đăng nhập
     * @param targetProfileId ID mục tiêu AIM được chọn
     * @return {@link SelectTargetResponse} chứa chỉ dẫn điều hướng tương ứng
     */
    SelectTargetResponse selectTarget(Long userId, Long targetProfileId);
}
