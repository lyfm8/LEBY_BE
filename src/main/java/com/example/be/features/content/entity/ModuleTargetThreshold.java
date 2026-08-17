package com.example.be.features.content.entity;

import com.example.be.features.ability.entity.TargetProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngưỡng điểm đỗ (Pass Score) của một Module theo từng Mục tiêu (Target Profile / AIM).
 * Giúp cá nhân hóa điểm đỗ: Người aim 550 chỉ cần 60 điểm, người aim 990 cần 80 điểm.
 */
@Entity
@Table(name = "module_target_thresholds")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTargetThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * passScore: Integer - Điểm tối thiểu để được tính là PASS module này cho AIM tương ứng.
     */
    @Column(nullable = false)
    private Integer passScore;

    /**
     * ModuleTargetThreshold * ---- 1 Module.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * ModuleTargetThreshold * ---- 1 TargetProfile.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;
}
