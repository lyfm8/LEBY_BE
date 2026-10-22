package com.example.be.entity.ability;

import com.example.be.entity.base.AbstractEntity;
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
public class AbilityEvaluationRule extends AbstractEntity {

    /**
     * level1: Float - ngưỡng mức 1 (thấp nhất - WEAK)
     */
    private Float level1;

    /**
     * level2: Float - ngưỡng mức 2 (DEVELOPING)
     */
    private Float level2;

    /**
     * level3: Float - ngưỡng mức 3 (cao nhất - STABLE)
     */
    private Float level3;

    /**
     * minEvidenceCount: Int - số lần tối thiểu để kết luận (đủ tin cậy)
     */
    @Column(nullable = false)
    private Integer minEvidenceCount;

}
