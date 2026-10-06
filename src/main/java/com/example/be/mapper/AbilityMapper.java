package com.example.be.mapper;

import com.example.be.dto.request.ability.AbilityRequest;
import com.example.be.dto.response.ability.AbilityResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import org.springframework.stereotype.Component;

@Component
public class AbilityMapper {

    public AbilityResponse toResponse(Ability ability) {
        if (ability == null) {
            return null;
        }
        return AbilityResponse.builder()
                .id(ability.getId())
                .name(ability.getName())
                .description(ability.getDescription())
                .sections(ability.getSections())
                .partId(ability.getPart() != null ? ability.getPart().getId() : null)
                .build();
    }

    public Ability toEntity(AbilityRequest request, Part part) {
        if (request == null) {
            return null;
        }
        Ability ability = new Ability();
        ability.setName(request.getName());
        ability.setDescription(request.getDescription());
        ability.setPart(part);
        if (part != null) {
            ability.setSections(part.getSections());
        }
        return ability;
    }
}
