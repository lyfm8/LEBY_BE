package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.user.TargetProfileRequest;
import com.example.be.dto.response.user.TargetProfileResponse;
import com.example.be.service.user.service.TargetProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.CREATED)
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
