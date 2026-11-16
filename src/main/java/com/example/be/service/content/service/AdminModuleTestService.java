package com.example.be.service.content.service;

import com.example.be.dto.request.content.AutoSelectModuleTestQuestionsRequest;
import com.example.be.dto.request.content.SaveModuleTestConfigRequest;
import com.example.be.dto.response.content.ModuleTestConfigDetailResponse;
import com.example.be.dto.response.content.ModuleTestOverviewItemResponse;
import com.example.be.dto.response.content.ModuleTestQuestionItemResponse;

import java.util.List;

/**
 * Giao diện nghiệp vụ Quản lý Bài thi vượt ải mô-đun (Use Case A07).
 */
public interface AdminModuleTestService {

    /**
     * Lấy danh sách tổng quan trạng thái bài thi vượt ải của tất cả các mô-đun.
     *
     * @return Danh sách trạng thái bài thi vượt ải sắp xếp theo thứ tự sư phạm (sequence)
     */
    List<ModuleTestOverviewItemResponse> getAllModuleTestOverviews();

    /**
     * Lấy chi tiết cấu hình bài thi vượt ải và danh sách câu hỏi hiện có của một mô-đun.
     *
     * @param moduleId ID của mô-đun cần xem cấu hình
     * @return ModuleTestConfigDetailResponse đầy đủ tham số và danh sách câu hỏi
     */
    ModuleTestConfigDetailResponse getModuleTestConfig(Long moduleId);

    /**
     * Lưu cấu hình tham số và danh sách câu hỏi cho bài thi vượt ải mô-đun.
     *
     * @param moduleId ID của mô-đun cần cập nhật
     * @param request  Dữ liệu cấu hình thời gian, số câu, xáo trộn, chế độ chọn đề và danh sách câu hỏi
     * @return ModuleTestConfigDetailResponse sau khi lưu thành công
     */
    ModuleTestConfigDetailResponse saveModuleTestConfig(Long moduleId, SaveModuleTestConfigRequest request);

    /**
     * Tự động bốc ngẫu nhiên danh sách câu hỏi khả dụng từ ngân hàng câu hỏi cho mô-đun (Luồng 6a).
     *
     * @param moduleId ID của mô-đun
     * @param request  Số lượng câu cần bốc và cờ lọc theo Ability của mô-đun
     * @return Danh sách câu hỏi ngẫu nhiên được đề xuất
     */
    List<ModuleTestQuestionItemResponse> autoSelectQuestions(
            Long moduleId,
            AutoSelectModuleTestQuestionsRequest request
    );
}
