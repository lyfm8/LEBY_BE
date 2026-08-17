package com.example.be.features.ability.entity;
import com.example.be.features.user.entity.LearningPath;

import com.example.be.features.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Kế hoạch mục tiêu Ability của User (AIM = Ability Improvement Map).
 * User chọn AIM để hệ thống generate LearningPath phù hợp.
 * Rule 25: AbilityAimPlan là Domain Entity, chooseAIM() là operation không phải Entity.
 */
@Entity
@Table(name = "ability_aim_plans")
@Getter
@Setter
@NoArgsConstructor
public class AbilityAimPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;

    @OneToMany(
            mappedBy = "plan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AbilityAimPlanItem> items = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
