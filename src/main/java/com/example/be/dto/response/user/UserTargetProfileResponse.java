package com.example.be.dto.response.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin một mốc mục tiêu điểm TOEIC (AIM) hiển thị cho học viên trên màn hình chọn AIM (UC05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTargetProfileResponse {

    private Long id;

    private Integer targetTotalScore;

    private String description;

    private Boolean status;

    @JsonProperty("isRecommended")
    private Boolean isRecommended;
}
