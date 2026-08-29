package com.example.be.common.security;

/**
 * Constants cho annotation @PreAuthorize — dùng thống nhất trong toàn bộ Controller.
 *
 * Mục đích: Tránh hardcode string role rải rác, dễ refactor khi tên role thay đổi.
 *
 * Cách dùng:
 * <pre>
 *     @PreAuthorize(Roles.ADMIN)
 *     @GetMapping("/api/admin/users")
 *     public ResponseEntity<?> getAllUsers() { ... }
 *
 *     @PreAuthorize(Roles.TEACHER_OR_ADMIN)
 *     @PostMapping("/api/questions")
 *     public ResponseEntity<?> createQuestion() { ... }
 *
 *     @PreAuthorize(Roles.ANY_AUTHENTICATED)
 *     @GetMapping("/api/auth/me")
 *     public ResponseEntity<?> me() { ... }
 * </pre>
 *
 * Lưu ý: Spring Security dùng prefix "ROLE_" nội bộ. hasRole('ADMIN') tự động
 * tương ứng với GrantedAuthority "ROLE_ADMIN" được set trong JwtAuthenticationFilter.
 */
public final class Roles {

    // -----------------------------------------------------------------------
    // Single Role
    // -----------------------------------------------------------------------

    /** Chỉ dành cho học viên TOEIC. */
    public static final String STUDENT = "hasRole('STUDENT')";

    /** Dành cho giáo viên / người biên soạn nội dung. */
    public static final String TEACHER = "hasRole('TEACHER')";

    /** Dành cho quản trị viên hệ thống. */
    public static final String ADMIN = "hasRole('ADMIN')";

    // -----------------------------------------------------------------------
    // Combined Roles
    // -----------------------------------------------------------------------

    /** TEACHER hoặc ADMIN — thường dùng cho CRUD nội dung học. */
    public static final String TEACHER_OR_ADMIN = "hasAnyRole('TEACHER', 'ADMIN')";

    /** STUDENT hoặc TEACHER hoặc ADMIN — mọi user đã đăng nhập. */
    public static final String ANY_AUTHENTICATED = "isAuthenticated()";

    // -----------------------------------------------------------------------

    private Roles() {
        // Utility class — không khởi tạo.
    }
}
