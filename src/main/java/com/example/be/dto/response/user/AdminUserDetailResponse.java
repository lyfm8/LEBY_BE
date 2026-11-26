package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp thông tin chi tiết tài khoản và toàn bộ tiến trình học tập của học viên (UC A02 - Bước 6).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {
    private UserResponse profile;
    private UserActiveExamStatusResponse activeExamStatus;
    private List<AdminUserDiagnosticHistoryItem> diagnosticHistory;
    private AdminUserLearningPathResponse learningPath;
    private List<AdminUserModuleTestHistoryItem> moduleTestHistory;
    private List<AdminUserMockTestHistoryItem> mockTestHistory;
}
