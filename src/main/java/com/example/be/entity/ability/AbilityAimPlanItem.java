package com.example.be.entity.ability;

import com.example.be.entity.content.Module;
import com.example.be.enums.content.ELessonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một item trong AbilityAimPlan, đại diện cho một Ability cần cải thiện.
 * Rule 25: AbilityAimPlanItem là Domain Entity.
 * Rule 28: orderNo là ordering nghiệp vụ, không được xóa.
 */
@Entity
@Table(name = "ability_aim_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class AbilityAimPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ELessonStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private AbilityAimPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = true)
    private Module module;
}
