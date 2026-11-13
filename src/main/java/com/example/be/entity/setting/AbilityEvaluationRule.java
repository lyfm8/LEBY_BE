package com.example.be.entity.setting;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.user.TargetProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quy tắc đánh giá Năng lực (Ability) của User theo từng AIM (TargetProfile).
 * Lưu trữ trạng thái và quan hệ dữ liệu phục vụ phân loại nhị phân Tầng 2.
 *
 * TRỤC ĐÁNH GIÁ NĂNG LỰC ABILITY:
 *   0%                                   stableMin                        100%
 *   |────────────── WEAK ────────────────────|────────────── STABLE ─────────|
 *   [Dưới ngưỡng: Bốc module vào lộ trình]   [Đạt chuẩn: Miễn học module]
 */
@Entity
@Table(
        name = "ability_evaluation_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ability_rule_ability_target",
                columnNames = {"ability_id", "target_profile_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AbilityEvaluationRule extends AbstractEntity {

    /**
     * Ngưỡng tối thiểu để đạt trạng thái STABLE (vững vàng) cho năng lực này theo AIM tương ứng.
     * Quy tắc đặt tên tuân thủ leby-anticonflict.md: stableMin.
     */
    @Column(name = "stable_min", nullable = false)
    private Float stableMin;

    /**
     * Quy tắc này áp dụng cho Năng lực (Ability) cụ thể nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = false)
    private Ability ability;

    /**
     * Quy tắc này áp dụng cho Mục tiêu (AIM / TargetProfile) nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;
}
