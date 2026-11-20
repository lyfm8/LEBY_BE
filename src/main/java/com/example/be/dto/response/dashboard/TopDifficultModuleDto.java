package com.example.be.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin mô-đun có tỷ lệ thi trượt cao nhất để Quản trị viên theo dõi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDifficultModuleDto {
    private Long moduleId;
    private String moduleTitle;
    private String partName;
    private long totalAttempts;
    private long failCount;
    private double failPercent;
}
