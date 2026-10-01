package com.example.be.service.ability.implement;

import com.example.be.dto.request.target.TargetProfileRequest;
import com.example.be.dto.response.target.TargetProfileResponse;
import com.example.be.entity.ability.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.ability.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.ability.TargetProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TargetProfileServiceImpl implements TargetProfileService {

    private final TargetProfileRepository targetProfileRepository;
    private final UserRepository userRepository;

    @Override
    public List<TargetProfileResponse> getAllProfiles() {
        return targetProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TargetProfileResponse createProfile(TargetProfileRequest request) {
        TargetProfile profile = new TargetProfile();
        profile.setTargetTotalScore(request.getAimScore());
        profile.setDescription(request.getDescription());
        profile.setStatus(true);

        profile = targetProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public TargetProfileResponse updateProfile(Long id, TargetProfileRequest request) {
        TargetProfile profile = targetProfileRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Target Profile not found"));

        profile.setTargetTotalScore(request.getAimScore());
        profile.setDescription(request.getDescription());

        profile = targetProfileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        TargetProfile profile = targetProfileRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Target Profile not found"));

        long userCount = userRepository.countByTargetProfileId(id);
        if (userCount > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Cannot delete profile because it is used by " + userCount + " user(s).");
        }

        targetProfileRepository.delete(profile);
    }

    private TargetProfileResponse mapToResponse(TargetProfile profile) {
        long totalUsers = userRepository.countByTargetProfileId(profile.getId());
        return TargetProfileResponse.builder()
                .id(profile.getId())
                .aimScore(profile.getTargetTotalScore())
                .description(profile.getDescription())
                .totalUsers(totalUsers)
                .build();
    }
}
