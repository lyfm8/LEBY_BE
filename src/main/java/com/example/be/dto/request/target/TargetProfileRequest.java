package com.example.be.dto.request.target;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetProfileRequest {
    private Integer aimScore;
    private String description;
}
