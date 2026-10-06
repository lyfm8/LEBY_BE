package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.request.content.PartRequest;
import com.example.be.dto.response.ability.AbilityResponse;
import com.example.be.dto.response.content.PartResponse;
import com.example.be.service.ability.service.AbilityService;
import com.example.be.service.part.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/parts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartController {

    private final PartService partService;
    private final AbilityService abilityService;

    @GetMapping
    public ApiResponse<List<PartResponse>> getAllParts() {
        return new ApiResponse<>(true, "Success", partService.getAllParts());
    }

    @GetMapping("/{id}")
    public ApiResponse<PartResponse> getPartById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Success", partService.getPartById(id));
    }

    @PostMapping
    public ApiResponse<PartResponse> createPart(@Valid @RequestBody PartRequest request) {
        return new ApiResponse<>(true, "Created successfully", partService.createPart(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PartResponse> updatePart(@PathVariable Long id, @Valid @RequestBody PartRequest request) {
        return new ApiResponse<>(true, "Updated successfully", partService.updatePart(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePart(@PathVariable Long id) {
        partService.deletePart(id);
        return new ApiResponse<>(true, "Deleted successfully");
    }

    @GetMapping("/{partId}/abilities")
    public ApiResponse<List<AbilityResponse>> getAbilitiesByPartId(@PathVariable Long partId) {
        return new ApiResponse<>(true, "Success", abilityService.getAbilitiesByPartId(partId));
    }

    @PostMapping("/{partId}/abilities")
    public ApiResponse<AbilityResponse> createAbility(@PathVariable Long partId, @Valid @RequestBody AbilityRequest request) {
        return new ApiResponse<>(true, "Created successfully", abilityService.createAbility(partId, request));
    }
}
