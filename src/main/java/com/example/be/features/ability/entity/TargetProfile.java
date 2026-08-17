package com.example.be.features.ability.entity;
import com.example.be.features.content.entity.Part;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Hồ sơ mục tiêu TOEIC của user (điểm tổng, điểm từng Part).
 * Admin có thể tạo sẵn các TargetProfile (ví dụ 500, 600, 700, 800).
 * Rule 25: TargetProfile là Domain Entity, không phải Use Case operation.
 */
@Entity
@Table(name = "target_profiles")
@Getter
@Setter
@NoArgsConstructor
public class TargetProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer targetTotalScore;

    @Column(nullable = false)
    private Boolean status = true;

    /**
     * TargetProfile 1 ---- * TargetPartThreshold (composition).
     * Rule 13: Dùng CascadeType.ALL + orphanRemoval vì TargetPartThreshold phụ thuộc TargetProfile.
     */
    @OneToMany(
            mappedBy = "targetProfile",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<TargetPartThreshold> thresholds = new ArrayList<>();
}
