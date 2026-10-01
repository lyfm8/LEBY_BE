package com.example.be.service.ability;

import com.example.be.dto.request.target.TargetProfileRequest;
import com.example.be.dto.response.target.TargetProfileResponse;

import java.util.List;

public interface TargetProfileService {
    List<TargetProfileResponse> getAllProfiles();
    TargetProfileResponse createProfile(TargetProfileRequest request);
    TargetProfileResponse updateProfile(Long id, TargetProfileRequest request);
    void deleteProfile(Long id);
}
