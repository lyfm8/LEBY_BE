package com.example.be.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một dòng lịch sử thi vượt ải mô-đun của học viên (UC A02 - Bước 6).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserModuleTestHistoryItem {
    private Long attemptId;
    private Long moduleId;
    private String moduleTitle;
    private String partName;
    private Integer score;
    private Integer passScore;
    private String result;
    private Boolean isSubmitted;
    private LocalDateTime submittedAt;
}
