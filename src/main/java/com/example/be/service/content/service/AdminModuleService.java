package com.example.be.service.content.service;

import com.example.be.dto.request.content.CreateModuleRequest;
import com.example.be.dto.request.content.ReorderModulesRequest;
import com.example.be.dto.request.content.UpdateModuleRequest;
import com.example.be.dto.request.content.UpdateModuleThresholdsRequest;
import com.example.be.dto.response.content.ModuleDetailResponse;
import com.example.be.dto.response.content.ModuleSummaryResponse;

import java.util.List;

/**
 * Service quản lý Module học tập dành cho Quản trị viên và Giáo viên (Use Case A05).
 */
public interface AdminModuleService {

    /**
     * Lấy danh sách toàn bộ các Module học tập kèm danh sách bài học con,
     * sắp xếp theo thứ tự hiển thị sư phạm từ nhỏ đến lớn.
     *
     * @return Danh sách các Module tóm tắt phục vụ cây điều hướng 25% bên trái
     */
    List<ModuleSummaryResponse> getAllModules();

    /**
     * Lấy thông tin chi tiết đầy đủ của một Module học tập theo ID.
     *
     * @param id ID của Module
     * @return Chi tiết Module
     */
    ModuleDetailResponse getModuleById(Long id);

    /**
     * Tạo mới một Module học tập.
     *
     * @param request Thông tin tạo mới Module
     * @return Chi tiết Module vừa tạo
     */
    ModuleDetailResponse createModule(CreateModuleRequest request);

    /**
     * Cập nhật thông tin toàn diện của một Module học tập hiện có.
     *
     * @param id ID của Module cần cập nhật
     * @param request Thông tin cập nhật Module
     * @return Chi tiết Module sau khi cập nhật
     */
    ModuleDetailResponse updateModule(Long id, UpdateModuleRequest request);

    /**
     * Cập nhật nhanh các tham số động của Module (Điểm đỗ mặc định và Tỷ lệ bài giảng hoàn thành).
     * Phục vụ giao diện "Cấu hình thông số" tập trung.
     *
     * @param id ID của Module
     * @param request Tham số ngưỡng mới
     * @return Chi tiết Module sau khi cập nhật
     */
    ModuleDetailResponse updateModuleThresholds(Long id, UpdateModuleThresholdsRequest request);

    /**
     * Xóa Module học tập.
     * Chặn xóa trực tiếp nếu Module đang nằm trong lộ trình của học viên (Luồng 2c).
     *
     * @param id ID của Module cần xóa
     */
    void deleteModule(Long id);

    /**
     * Sắp xếp lại thứ tự sư phạm tuần tự giữa các Module trong hệ thống.
     *
     * @param request Danh sách ID các Module theo thứ tự mới
     */
    void reorderModules(ReorderModulesRequest request);
}

