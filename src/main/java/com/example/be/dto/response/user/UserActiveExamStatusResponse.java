package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi trạng thái phiên thi trực tuyến đang diễn ra của học viên (UC A02 - Ngoại lệ 7d).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveExamStatusResponse {
    private boolean hasActiveExamSession;
    private boolean hasActiveDiagnostic;
    private boolean hasActiveModuleTest;
}
