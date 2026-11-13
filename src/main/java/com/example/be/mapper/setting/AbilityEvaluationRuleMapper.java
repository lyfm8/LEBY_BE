package com.example.be.mapper.setting;

import com.example.be.dto.request.setting.CreateAbilityEvaluationRuleRequest;
import com.example.be.dto.request.setting.UpdateAbilityEvaluationRuleRequest;
import com.example.be.dto.response.setting.AbilityEvaluationRuleResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.AbilityEvaluationRule;
import org.springframework.stereotype.Component;

@Component
public class AbilityEvaluationRuleMapper {

    public AbilityEvaluationRuleResponse toResponse(AbilityEvaluationRule entity) {
        if (entity == null) {
            return null;
        }

        Ability ability = entity.getAbility();
        Part part = ability != null ? ability.getPart() : null;

        return AbilityEvaluationRuleResponse.builder()
                .id(entity.getId())
                .abilityId(ability != null ? ability.getId() : null)
                .abilityName(ability != null ? ability.getName() : null)
                .partId(part != null ? part.getId() : null)
                .partName(part != null ? part.getName() : null)
                .stableMin(entity.getStableMin())
                .developingMax(entity.getDevelopingMax())
                .weakMax(entity.getWeakMax())
                .minEvidenceCount(entity.getMinEvidenceCount())
                .build();
    }

    public AbilityEvaluationRule toEntity(CreateAbilityEvaluationRuleRequest request, Ability ability) {
        if (request == null) {
            return null;
        }

        AbilityEvaluationRule entity = new AbilityEvaluationRule();
        entity.setAbility(ability);
        entity.setStableMin(request.getStableMin());
        entity.setWeakMax(request.getWeakMax());
        entity.setMinEvidenceCount(request.getMinEvidenceCount());
        return entity;
    }

    public void updateEntity(AbilityEvaluationRule entity, UpdateAbilityEvaluationRuleRequest request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setStableMin(request.getStableMin());
        entity.setWeakMax(request.getWeakMax());
        entity.setMinEvidenceCount(request.getMinEvidenceCount());
    }
}
