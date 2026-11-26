package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.UpdateUserRoleRequest;
import com.example.be.dto.response.user.AdminUserDetailResponse;
import com.example.be.dto.response.user.UserActiveExamStatusResponse;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.security.Roles;
import com.example.be.service.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Quản trị tài khoản người dùng (UC A02).
 * Chỉ tài khoản có vai trò ADMIN mới được phép truy cập toàn bộ các endpoint trong controller này.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN)
public class AdminUserController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXPORT_FILENAME = "danh-sach-nguoi-dung.xlsx";

    private final UserService userService;

    /**
     * Lấy danh sách tài khoản người dùng có phân trang và bộ lọc.
     *
     * @param page     Trang hiện tại (bắt đầu từ 0)
     * @param size     Số dòng trên mỗi trang
     * @param keyword  Từ khóa tìm kiếm theo họ tên hoặc email
     * @param role     Lọc theo vai trò (STUDENT / TEACHER / ADMIN)
     * @param isActive Lọc theo trạng thái kích hoạt (true / false)
     * @param aimScore Lọc theo mốc điểm mục tiêu AIM (450, 550, 650, ...)
     * @return ApiResponse chứa danh sách tài khoản và thông tin phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer aimScore
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> result = userService.getAllUsers(keyword, role, isActive, aimScore, pageable);

        ApiResponse.Pagination pagination = ApiResponse.Pagination.builder()
                .page(result.getNumber())
                .pageSize(result.getSize())
                .totalItems((int) result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .data(result.getContent())
                .pagination(pagination)
                .build());
    }

    /**
     * Xuất toàn bộ danh sách tài khoản người dùng khớp bộ lọc hiện tại ra file Excel (.xlsx).
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer aimScore
    ) {
        byte[] excelBytes = userService.exportUsersToExcel(keyword, role, isActive, aimScore);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + EXPORT_FILENAME + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }

    /**
     * Xem chi tiết thông tin tài khoản và tiến trình học tập của người dùng (UC A02 - Bước 5, 6 & Luồng 7b).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<AdminUserDetailResponse>builder()
                .success(true)
                .data(userService.getUserDetail(id))
                .build());
    }

    /**
     * Kiểm tra người dùng có đang trong phiên thi trực tuyến nào không trước khi Admin thao tác khóa (Ngoại lệ 7d).
     */
    @GetMapping("/{id}/active-exam-status")
    public ResponseEntity<ApiResponse<UserActiveExamStatusResponse>> getActiveExamStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserActiveExamStatusResponse>builder()
                .success(true)
                .data(userService.getActiveExamStatus(id))
                .build());
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (tuân thủ BR11 và ngoại lệ 7d).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        UserResponse updated = userService.toggleUserStatus(id, force);
        String message = Boolean.TRUE.equals(updated.getIsActive())
                ? "Mở khóa tài khoản thành công"
                : "Tạm khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message(message)
                .data(updated)
                .build());
    }

    /**
     * Nâng quyền hoặc hạ quyền qua lại giữa STUDENT và TEACHER (UC A02 - Luồng 7c).
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse updated = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Cập nhật vai trò người dùng thành công")
                .data(updated)
                .build());
    }

    /**
     * Sinh mật khẩu ngẫu nhiên đạt chuẩn và gửi email thông báo cho học viên (UC A02 - Luồng 7a).
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(@PathVariable Long id) {
        userService.resetUserPassword(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đã đặt lại mật khẩu mới và gửi hướng dẫn qua email người dùng")
                .build());
    }
}
