package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.response.ability.AbilityResponse;
import com.example.be.service.ability.service.AbilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/abilities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAbilityController {

    private final AbilityService abilityService;

    @PutMapping("/{id}")
    public ApiResponse<AbilityResponse> updateAbility(@PathVariable Long id, @Valid @RequestBody AbilityRequest request) {
        return new ApiResponse<>(true, "Updated successfully", abilityService.updateAbility(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAbility(@PathVariable Long id) {
        abilityService.deleteAbility(id);
        return new ApiResponse<>(true, "Deleted successfully");
    }
}
