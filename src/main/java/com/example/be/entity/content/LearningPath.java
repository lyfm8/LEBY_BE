package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class LearningPath extends AbstractEntity {

    /**
     * version: Int - phiên bản lộ trình (từ Class Diagram)
     */
    private Integer version;

    /**
     * status: Boolean - trạng thái active của lộ trình
     */
    @Column(nullable = false)
    private Boolean status = true;

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
