package com.example.be.dto.response.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleThresholdMatrixResponse {
    private List<Long> profileIds;
    private List<Integer> profileScores;
    private List<String> profileNames;
    private List<ModuleThresholdMatrixRowResponse> rows;
}
