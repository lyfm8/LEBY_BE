package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.exam.AutoSelectQuestionsRequest;
import com.example.be.dto.request.exam.CreateExamRequest;
import com.example.be.dto.request.exam.UpdateExamRequest;
import com.example.be.dto.response.exam.AutoSelectQuestionsResponse;
import com.example.be.dto.response.exam.ExamDetailResponse;
import com.example.be.dto.response.exam.ExamListItemResponse;
import com.example.be.dto.response.exam.ExamMatrixAvailabilityResponse;
import com.example.be.entity.user.User;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.security.Roles;
import com.example.be.service.exam.service.AdminExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller phục vụ module Quản lý Đề thi (Diagnostic Test & Mock Test).
 * Phân quyền chặt chẽ giữa Quản trị viên (Admin) và Giáo viên (Teacher).
 */
@Validated
@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private final AdminExamService adminExamService;

    /**
     * Lấy danh sách đề thi kèm phân trang và lọc đa tiêu chí.
     * Cả Giáo viên và Admin đều có quyền xem.
     */
    @GetMapping
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<List<ExamListItemResponse>>> getExams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ETestType testType,
            @RequestParam(required = false) EExamStatus examStatus
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<ExamListItemResponse> result = adminExamService.getExams(keyword, testType, examStatus, pageable);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(result.getNumber())
                .pageSize(result.getSize())
                .totalItems((int) result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.<List<ExamListItemResponse>>builder()
                .success(true)
                .message("Lấy danh sách đề thi thành công")
                .data(result.getContent())
                .pagination(pagination)
                .build());
    }

    /**
     * Lấy số lượng đề thi trên các Tabs (Tất cả, Chẩn đoán, Thi thử).
     */
    @GetMapping("/tab-counts")
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<Map<String, Long>>> getExamTabCounts() {
        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .success(true)
                .message("Lấy số lượng đề thi theo Tabs thành công")
                .data(adminExamService.getExamTabCounts())
                .build());
    }

    /**
     * Xem chi tiết thông tin bộ đề thi kèm danh sách câu hỏi.
     */
    @GetMapping("/{id}")
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<ExamDetailResponse>> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .message("Lấy chi tiết đề thi thành công")
                .data(adminExamService.getExamById(id))
                .build());
    }

    /**
     * Tạo mới đề thi (DRAFT hoặc ARCHIVE).
     * Cả Giáo viên và Admin đều có quyền tạo đề.
     */
    @PostMapping
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<ExamDetailResponse>> createExam(
            @Valid @RequestBody CreateExamRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        String username = currentUser != null ? currentUser.getFullName() : "Admin";
        ExamDetailResponse response = adminExamService.createExam(request, username);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ExamDetailResponse>builder()
                        .success(true)
                        .message("Tạo đề thi mới thành công")
                        .data(response)
                        .build());
    }

    /**
     * Cập nhật thông tin đề thi.
     * Cả Giáo viên và Admin đều có quyền sửa đề.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<ExamDetailResponse>> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        String username = currentUser != null ? currentUser.getFullName() : "Admin";
        ExamDetailResponse response = adminExamService.updateExam(id, request, username);

        return ResponseEntity.ok(ApiResponse.<ExamDetailResponse>builder()
                .success(true)
                .message("Cập nhật đề thi thành công")
                .data(response)
                .build());
    }

    /**
     * Kích hoạt đề thi (ACTIVE).
     * 🔒 QUYỀN HẠN: CHỈ ADMIN MỚI ĐƯỢC PHÉP KÍCH HOẠT.
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<ApiResponse<Void>> activateExam(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        String username = currentUser != null ? currentUser.getFullName() : "Admin";
        adminExamService.activateExam(id, username);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Kích hoạt đề thi thành công")
                .build());
    }

    /**
     * Lưu trữ đề thi (ARCHIVE).
     * 🔒 QUYỀN HẠN: CHỈ ADMIN MỚI ĐƯỢC PHÉP LƯU TRỮ.
     */
    @PatchMapping("/{id}/archive")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<ApiResponse<Void>> archiveExam(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        String username = currentUser != null ? currentUser.getFullName() : "Admin";
        adminExamService.archiveExam(id, username);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Lưu trữ đề thi thành công")
                .build());
    }

    /**
     * Xóa đề thi (chỉ cho phép nếu chưa có lượt thi nào).
     * 🔒 QUYỀN HẠN: CHỈ ADMIN MỚI ĐƯỢC PHÉP XÓA.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<ApiResponse<Void>> deleteExam(@PathVariable Long id) {
        adminExamService.deleteExam(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa đề thi thành công")
                .build());
    }

    /**
     * Lấy số lượng câu hỏi khả dụng hiện có theo ma trận và Abilities.
     * Cung cấp dữ liệu cho Bảng Ma trận và Checklist trước khi lưu.
     */
    @GetMapping("/matrix-availability")
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<ExamMatrixAvailabilityResponse>> getMatrixAvailability() {
        return ResponseEntity.ok(ApiResponse.<ExamMatrixAvailabilityResponse>builder()
                .success(true)
                .message("Lấy thông tin khả dụng ma trận thành công")
                .data(adminExamService.getMatrixAvailability())
                .build());
    }

    /**
     * Tự động chọn câu hỏi ngẫu nhiên từ Question Bank thỏa mãn ma trận.
     */
    @PostMapping("/auto-select")
    @PreAuthorize(Roles.TEACHER_OR_ADMIN)
    public ResponseEntity<ApiResponse<AutoSelectQuestionsResponse>> autoSelectQuestions(
            @Valid @RequestBody AutoSelectQuestionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<AutoSelectQuestionsResponse>builder()
                .success(true)
                .message("Tự động chọn câu hỏi theo ma trận thành công")
                .data(adminExamService.autoSelectQuestions(request))
                .build());
    }
}
