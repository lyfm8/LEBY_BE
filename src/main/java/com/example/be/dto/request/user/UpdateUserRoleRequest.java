package com.example.be.dto.request.user;

import com.example.be.enums.user.ERole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO cập nhật vai trò người dùng bởi Quản trị viên (UC A02 - Luồng nâng/hạ quyền).
 * Nghiệp vụ tại Service chỉ cho phép chuyển đổi qua lại giữa STUDENT và TEACHER.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "Vai trò mới không được để trống")
    private ERole role;
}
