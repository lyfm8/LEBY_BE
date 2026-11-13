package com.example.be.mapper.setting;

import com.example.be.dto.request.setting.ModuleThresholdItemDto;
import com.example.be.dto.response.setting.ModuleTargetThresholdResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import org.springframework.stereotype.Component;

@Component
public class ModuleTargetThresholdMapper {

    public ModuleTargetThresholdResponse toResponse(ModuleTargetThreshold entity) {
        if (entity == null) {
            return null;
        }

        Module module = entity.getModule();
        TargetProfile profile = entity.getTargetProfile();

        return ModuleTargetThresholdResponse.builder()
                .id(entity.getId())
                .moduleId(module != null ? module.getId() : null)
                .moduleTitle(module != null ? module.getTitle() : null)
                .targetProfileId(profile != null ? profile.getId() : null)
                .targetProfileScore(profile != null ? profile.getScore() : null)
                .targetProfileName(profile != null ? profile.getName() : null)
                .passScore(entity.getPassScore())
                .build();
    }

    public ModuleTargetThreshold toEntity(ModuleThresholdItemDto dto, Module module, TargetProfile targetProfile) {
        if (dto == null) {
            return null;
        }

        ModuleTargetThreshold entity = new ModuleTargetThreshold();
        entity.setModule(module);
        entity.setTargetProfile(targetProfile);
        entity.setPassScore(dto.getPassScore());
        return entity;
    }
}
