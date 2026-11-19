package com.example.be.service.setting.service;

import com.example.be.dto.request.setting.CreateTargetPartThresholdRequest;
import com.example.be.dto.request.setting.UpdateTargetPartThresholdRequest;
import com.example.be.dto.response.setting.TargetPartThresholdResponse;

import java.util.List;

public interface TargetPartThresholdService {

    List<TargetPartThresholdResponse> getAllThresholds();

    List<TargetPartThresholdResponse> getThresholdsByProfile(Long targetProfileId);

    TargetPartThresholdResponse getThresholdById(Long id);

    TargetPartThresholdResponse createThreshold(CreateTargetPartThresholdRequest request);

    TargetPartThresholdResponse updateThreshold(Long id, UpdateTargetPartThresholdRequest request);

    void deleteThreshold(Long id);
}
