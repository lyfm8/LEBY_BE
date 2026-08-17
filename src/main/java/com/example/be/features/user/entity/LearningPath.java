package com.example.be.features.user.entity;
import com.example.be.features.ability.entity.AbilityAimPlan;
import com.example.be.features.user.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lộ trình học tập được sinh ra cho User dựa trên AbilityAimPlan.
 * Rule 24: LearningPath là persistent domain object, không phải Use Case.
 */
@Entity
@Table(name = "learning_paths")
@Getter
@Setter
@NoArgsConstructor
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * version: Int - phiên bản lộ trình (từ Class Diagram)
     */
    private Integer version;

    /**
     * status: Boolean - trạng thái active của lộ trình
     */
    @Column(nullable = false)
    private Boolean status = true;

    private LocalDate startedAt;

    private LocalDate endedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * LearningPath 1 ---- * LearningPathItem.
     * Composition: LearningPathItem phụ thuộc vòng đời vào LearningPath.
     * Rule 13: Dùng CascadeType.ALL + orphanRemoval vì composition.
     */
    @OneToMany(
            mappedBy = "learningPath",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LearningPathItem> items = new ArrayList<>();

}
