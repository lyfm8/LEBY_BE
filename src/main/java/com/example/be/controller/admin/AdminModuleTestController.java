package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.content.AutoSelectModuleTestQuestionsRequest;
import com.example.be.dto.request.content.SaveModuleTestConfigRequest;
import com.example.be.dto.response.content.ModuleTestConfigDetailResponse;
import com.example.be.dto.response.content.ModuleTestOverviewItemResponse;
import com.example.be.dto.response.content.ModuleTestQuestionItemResponse;
import com.example.be.service.content.service.AdminModuleTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller phục vụ Quản lý & Cấu hình Bài thi vượt ải mô-đun (Use Case A07).
 * URL prefix: /api/admin/module-tests
 */
@RestController
@RequestMapping("/api/admin/module-tests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminModuleTestController {

    private final AdminModuleTestService adminModuleTestService;

    /**
     * Lấy danh sách tổng quan trạng thái bài thi vượt ải của tất cả các mô-đun.
     */
    @GetMapping
    public ApiResponse<List<ModuleTestOverviewItemResponse>> getAllModuleTestOverviews() {
        return new ApiResponse<>(
                true,
                "Lấy danh sách bài thi vượt ải mô-đun thành công",
                adminModuleTestService.getAllModuleTestOverviews()
        );
    }

    /**
     * Lấy chi tiết cấu hình bài thi vượt ải và danh sách câu hỏi hiện có của một mô-đun.
     */
    @GetMapping("/{moduleId}")
    public ApiResponse<ModuleTestConfigDetailResponse> getModuleTestConfig(@PathVariable Long moduleId) {
        return new ApiResponse<>(
                true,
                "Lấy chi tiết cấu hình bài thi vượt ải thành công",
                adminModuleTestService.getModuleTestConfig(moduleId)
        );
    }

    /**
     * Lưu cấu hình tham số và danh sách câu hỏi cho bài thi vượt ải mô-đun.
     */
    @PutMapping("/{moduleId}")
    public ApiResponse<ModuleTestConfigDetailResponse> saveModuleTestConfig(
            @PathVariable Long moduleId,
            @Valid @RequestBody SaveModuleTestConfigRequest request
    ) {
        return new ApiResponse<>(
                true,
                "Lưu cấu hình bài thi vượt ải thành công",
                adminModuleTestService.saveModuleTestConfig(moduleId, request)
        );
    }

    /**
     * Tự động bốc ngẫu nhiên câu hỏi từ Ngân hàng câu hỏi đúng theo Ability của mô-đun (Luồng 6a).
     */
    @PostMapping("/{moduleId}/auto-select")
    public ApiResponse<List<ModuleTestQuestionItemResponse>> autoSelectQuestions(
            @PathVariable Long moduleId,
            @Valid @RequestBody AutoSelectModuleTestQuestionsRequest request
    ) {
        return new ApiResponse<>(
                true,
                "Tự động chọn câu hỏi thành công",
                adminModuleTestService.autoSelectQuestions(moduleId, request)
        );
    }
}
