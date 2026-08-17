package com.example.be.features.ability.entity;

import com.example.be.features.content.entity.Part;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngưỡng điểm của từng Part trong một TargetProfile.
 * Được dùng để đánh giá kết quả Diagnostic và quyết định directive (PASS/CONFIRM/...).
 * Rule 25: TargetPartThreshold là Domain Entity.
 */
@Entity
@Table(name = "target_part_thresholds")
@Getter
@Setter
@NoArgsConstructor
public class TargetPartThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * targetScore: Float - điểm mục tiêu cho Part này
     */
    @Column(nullable = false)
    private Float targetScore;

    /**
     * passScore: Float - điểm pass tối thiểu
     */
    @Column(nullable = false)
    private Float passScore;

    /**
     * confirmingScore: Float - điểm để confirm (không cần học thêm)
     */
    private Float confirmingScore;

    /**
     * fullPartScore: Float - điểm tối đa của Part
     */
    private Float fullPartScore;

    /**
     * TargetPartThreshold * ---- 1 TargetProfile.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;

    /**
     * TargetPartThreshold * ---- 1 Part (ngưỡng cho Part nào).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;
}
