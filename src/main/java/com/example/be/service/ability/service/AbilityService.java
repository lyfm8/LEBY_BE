package com.example.be.service.ability.service;

import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.response.ability.AbilityResponse;

import java.util.List;

public interface AbilityService {
    List<AbilityResponse> getAbilitiesByPartId(Long partId);
    AbilityResponse createAbility(Long partId, AbilityRequest request);
    AbilityResponse updateAbility(Long id, AbilityRequest request);
    void deleteAbility(Long id);
}
