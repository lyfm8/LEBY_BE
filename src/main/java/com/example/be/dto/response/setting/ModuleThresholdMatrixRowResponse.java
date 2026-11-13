package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleThresholdMatrixRowResponse {
    private Long moduleId;
    private String moduleTitle;
    private Integer moduleSequence;
    private Long partId;
    private String partName;
    private Integer defaultPassScore;
    private Map<String, Integer> thresholds;
    private Map<String, Boolean> isCustom;
}
