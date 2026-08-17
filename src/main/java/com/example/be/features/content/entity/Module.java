package com.example.be.features.content.entity;
import com.example.be.features.content.entity.ModuleTestAttempt;
import com.example.be.features.question.entity.ModuleTestQuestion;
import com.example.be.features.user.entity.LearningPath;
import com.example.be.features.content.entity.Part;
import com.example.be.features.ability.entity.Ability;

import com.example.be.features.content.enums.EModuleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Module học tập trong hệ thống LEBY.
 * Một LearningPath chứa nhiều Module.
 * Module có thể train via một hoặc nhiều Ability (quan hệ M-M qua ModuleAbility).
 * Rule 28: sequence là ordering nghiệp vụ.
 */
@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String type;

    /**
     * sequence: Int - thứ tự của module trong lộ trình
     * Rule 28: Không xóa ordering field.
     */
    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EModuleStatus status;

    /**
     * Module * ---- 1 Part (module thuộc về Part nào trong TOEIC).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = true)
    private Part part;

    /**
     * Module "trains via" Ability - M-M qua ModuleAbility.
     * Rule 12: Dùng entity trung gian ModuleAbility.
     */
    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY)
    private List<ModuleAbility> moduleAbilities = new ArrayList<>();

    /**
     * Danh sách điểm đỗ (Pass Score) thay đổi theo từng AIM.
     */
    @OneToMany(
            mappedBy = "module",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ModuleTargetThreshold> moduleTargetThresholds = new ArrayList<>();

    @OneToMany(
            mappedBy = "module",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ModuleTestQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "module", fetch = FetchType.LAZY)
    private List<ModuleTestAttempt> attempts = new ArrayList<>();
}
