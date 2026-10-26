package com.example.be.dto.request.content;

import com.example.be.enums.ability.ESection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartRequest {

    @NotBlank(message = "Tên Part không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Section không được để trống")
    private ESection sections;
}
