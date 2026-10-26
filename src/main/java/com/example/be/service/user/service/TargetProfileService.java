package com.example.be.service.user.service;

import com.example.be.dto.request.user.TargetProfileRequest;
import com.example.be.dto.response.user.TargetProfileResponse;

import java.util.List;

public interface TargetProfileService {
    List<TargetProfileResponse> getAllProfiles();
    TargetProfileResponse createProfile(TargetProfileRequest request);
    TargetProfileResponse updateProfile(Long id, TargetProfileRequest request);
    void deleteProfile(Long id);
}
