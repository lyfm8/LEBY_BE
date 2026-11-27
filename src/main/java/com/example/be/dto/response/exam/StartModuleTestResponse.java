package com.example.be.dto.response.exam;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Phản hồi khi khởi tạo hoặc khôi phục phiên thi vượt ải mô-đun (UC13).
 */
@Getter
@Builder
public class StartModuleTestResponse {

    private Long attemptId;
    private Long moduleId;
    private LocalDateTime startedAt;
    private Integer durationMinutes;
}
