package com.example.be.mapper.setting;

import com.example.be.dto.request.setting.CreateTargetPartThresholdRequest;
import com.example.be.dto.request.setting.UpdateTargetPartThresholdRequest;
import com.example.be.dto.response.setting.TargetPartThresholdResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import org.springframework.stereotype.Component;

@Component
public class TargetPartThresholdMapper {

    public TargetPartThresholdResponse toResponse(TargetPartThreshold entity) {
        if (entity == null) {
            return null;
        }

        TargetProfile profile = entity.getTargetProfile();
        Part part = entity.getPart();

        return TargetPartThresholdResponse.builder()
                .id(entity.getId())
                .targetProfileId(profile != null ? profile.getId() : null)
                .targetProfileScore(profile != null ? profile.getScore() : null)
                .targetProfileName(profile != null ? profile.getName() : null)
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .passScore(entity.getPassScore())
                .confirmingScore(entity.getConfirmingScore())
                .fullPartThreshold(entity.getFullPartThreshold())
                .build();
    }

    public TargetPartThreshold toEntity(CreateTargetPartThresholdRequest request, TargetProfile targetProfile, Part part) {
        if (request == null) {
            return null;
        }

        TargetPartThreshold entity = new TargetPartThreshold();
        entity.setTargetProfile(targetProfile);
        entity.setPart(part);
        entity.setPassScore(request.getPassScore());
        entity.setConfirmingScore(request.getConfirmingScore());
        entity.setFullPartThreshold(request.getFullPartThreshold());
        return entity;
    }

    public void updateEntity(TargetPartThreshold entity, UpdateTargetPartThresholdRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setPassScore(request.getPassScore());
        entity.setConfirmingScore(request.getConfirmingScore());
        entity.setFullPartThreshold(request.getFullPartThreshold());
    }
}
