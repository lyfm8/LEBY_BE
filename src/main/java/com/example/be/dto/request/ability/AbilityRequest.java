package com.example.be.dto.request.ability;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityRequest {

    @NotBlank(message = "Tên Ability không được để trống")
    private String name;

    private String description;
}
