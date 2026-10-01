package com.example.be.dto.response.target;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetProfileResponse {
    private Long id;
    private Integer aimScore;
    private String description;
    private Long totalUsers;
}
