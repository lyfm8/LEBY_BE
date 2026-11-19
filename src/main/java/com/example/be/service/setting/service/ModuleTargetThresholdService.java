package com.example.be.service.setting.service;

import com.example.be.dto.request.setting.BatchUpdateModuleThresholdRequest;
import com.example.be.dto.response.setting.ModuleTargetThresholdResponse;
import com.example.be.dto.response.setting.ModuleThresholdMatrixResponse;

import java.util.List;

public interface ModuleTargetThresholdService {

    ModuleThresholdMatrixResponse getMatrix(Long partId);

    List<ModuleTargetThresholdResponse> batchUpsert(BatchUpdateModuleThresholdRequest request);

    ModuleTargetThresholdResponse upsertSingle(Long moduleId, Long targetProfileId, Integer passScore);

    void resetToDefault(Long moduleId, Long targetProfileId);
}
