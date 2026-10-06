package com.example.be.dto.response.ability;

import com.example.be.enums.ability.ESection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityResponse {
    private Long id;
    private String name;
    private String description;
    private ESection sections;
    private Long partId;
}
