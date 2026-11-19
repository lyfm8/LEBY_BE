package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetPartThresholdResponse {
    private Long id;
    private Long targetProfileId;
    private Integer targetProfileScore;
    private String targetProfileName;
    private Long partId;
    private String partName;
    private Float passScore;
    private Float confirmingScore;
    private Float fullPartThreshold;
}
