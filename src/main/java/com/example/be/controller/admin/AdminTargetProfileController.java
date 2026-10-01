package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.target.TargetProfileRequest;
import com.example.be.dto.response.target.TargetProfileResponse;
import com.example.be.service.ability.TargetProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/target-profiles")
@RequiredArgsConstructor
public class AdminTargetProfileController {

    private final TargetProfileService targetProfileService;

    @GetMapping
    public ApiResponse<List<TargetProfileResponse>> getAllProfiles() {
        return new ApiResponse<>(true, "Success", targetProfileService.getAllProfiles());
    }

    @PostMapping
    public ApiResponse<TargetProfileResponse> createProfile(@RequestBody TargetProfileRequest request) {
        return new ApiResponse<>(true, "Created successfully", targetProfileService.createProfile(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TargetProfileResponse> updateProfile(@PathVariable Long id, @RequestBody TargetProfileRequest request) {
        return new ApiResponse<>(true, "Updated successfully", targetProfileService.updateProfile(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProfile(@PathVariable Long id) {
        targetProfileService.deleteProfile(id);
        return new ApiResponse<>(true, "Deleted successfully");
    }
}
