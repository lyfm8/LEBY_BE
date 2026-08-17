package com.example.be.features.ability.entity;
import com.example.be.features.user.entity.User;
import com.example.be.features.ability.entity.UserAbility;
import com.example.be.features.ability.entity.Ability;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rule đánh giá Ability của User dựa trên kết quả Diagnostic.
 * Được Admin cấu hình, Service dùng để evaluate UserAbility sau diagnostic.
 * Rule 25: AbilityEvaluationRule là Domain Entity, manageThreshold() là operation.
 */
@Entity
@Table(name = "ability_evaluation_rules")
@Getter
@Setter
@NoArgsConstructor
public class AbilityEvaluationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * weakMax: Float - ngưỡng trên của WEAK
     */
    private Float weakMax;

    /**
     * minEvidenceCount: Int - số lần tối thiểu để kết luận (đủ tin cậy)
     */
    @Column(nullable = false)
    private Integer minEvidenceCount;

    /**
     * developingMax: Float - ngưỡng trên của DEVELOPING
     */
    private Float developingMax;

    /**
     * stableMin: Float - ngưỡng dưới của STABLE
     */
    private Float stableMin;

}
