package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO phản hồi ma trận ngưỡng điểm 7 Part theo AIM đã chọn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AimPartThresholdMatrixResponse {

    private Long targetProfileId;
    private Integer aimScore;
    private String aimDescription;
    private List<PartThresholdRowResponse> rows;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartThresholdRowResponse {
        private Long id;
        private Long partId;
        private String partName;
        private Integer partNo;
        private String section;
        private Float passScore;
        private Float confirmingScore;
    }
}

