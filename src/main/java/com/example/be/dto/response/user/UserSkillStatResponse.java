package com.example.be.dto.response.user;

import com.example.be.enums.ability.EAbilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phản hồi thống kê từng năng lực (Ability) trong Ma trận năng lực của người dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillStatResponse {

    private Long abilityId;
    private String abilityName;
    private Integer partNo;
    private Float accuracyRate;
    private Integer evidenceCount;
    private EAbilityStatus status;
}
