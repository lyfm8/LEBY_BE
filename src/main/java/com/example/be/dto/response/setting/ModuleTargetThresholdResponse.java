package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleTargetThresholdResponse {
    private Long id;
    private Long moduleId;
    private String moduleTitle;
    private Long targetProfileId;
    private Integer targetProfileScore;
    private String targetProfileName;
    private Integer passScore;
}
