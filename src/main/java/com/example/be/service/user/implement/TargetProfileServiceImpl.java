package com.example.be.service.user.implement;

import com.example.be.dto.request.user.TargetProfileRequest;
import com.example.be.dto.response.user.TargetProfileResponse;
import com.example.be.entity.user.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.user.TargetProfileMapper;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.user.service.TargetProfileService;
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
    private final TargetProfileMapper targetProfileMapper;

    @Override
    public List<TargetProfileResponse> getAllProfiles() {
        return targetProfileRepository.findAllByOrderByTargetTotalScoreAsc().stream()
                .map(profile -> targetProfileMapper.toAdminResponse(
                        profile,
                        userRepository.countByTargetProfileId(profile.getId())
                ))
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
        return targetProfileMapper.toAdminResponse(profile, 0L);
    }

    @Override
    @Transactional
    public TargetProfileResponse updateProfile(Long id, TargetProfileRequest request) {
        TargetProfile profile = targetProfileRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.TARGET_NOT_FOUND, "Target Profile not found"));

        profile.setTargetTotalScore(request.getAimScore());
        profile.setDescription(request.getDescription());

        profile = targetProfileRepository.save(profile);
        long totalUsers = userRepository.countByTargetProfileId(profile.getId());
        return targetProfileMapper.toAdminResponse(profile, totalUsers);
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        TargetProfile profile = targetProfileRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.TARGET_NOT_FOUND, "Target Profile not found"));

        long userCount = userRepository.countByTargetProfileId(id);
        if (userCount > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Cannot delete profile because it is used by " + userCount + " user(s).");
        }

        targetProfileRepository.delete(profile);
    }
}
