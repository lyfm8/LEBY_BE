package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.content.CourseCatalogResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.content.service.UserCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API Thư viện khóa học 7 Part TOEIC cho học viên (UC17 Catalog).
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/users/catalog — Lấy toàn bộ cấu trúc 7 Part kèm phân quyền đối chiếu lộ trình cá nhân
 * </pre>
 */
@RestController
@RequestMapping("/api/users/catalog")
@RequiredArgsConstructor
public class UserCatalogController {

    private final UserCatalogService userCatalogService;

    /**
     * Lấy toàn bộ Thư viện khóa học 7 Part TOEIC kèm đề cương xem trước và trạng thái học tập
     * đối chiếu theo lộ trình hiện hành của người dùng đang đăng nhập.
     *
     * @param currentUser Người dùng đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @return {@link ApiResponse} chứa {@link CourseCatalogResponse}
     */
    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<CourseCatalogResponse>> getCourseCatalog(
            @AuthenticationPrincipal User currentUser
    ) {
        CourseCatalogResponse catalog = userCatalogService.getCourseCatalogForUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<CourseCatalogResponse>builder()
                .success(true)
                .message("Tải thư viện khóa học thành công")
                .data(catalog)
                .build());
    }
}
