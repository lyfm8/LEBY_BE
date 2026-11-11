package com.example.be.entity.content;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.enums.content.EModuleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Module học tập trong hệ thống LEBY (Use Case A05).
 * Một Module đại diện cho một chuyên đề học tập chuẩn sư phạm.
 * Hỗ trợ các tham số động: điểm đỗ mặc định (defaultPassScore) và tỷ lệ bài giảng tối thiểu (requiredCompletionRate).
 */
@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class Module extends AbstractEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String type;

    /**
     * Thứ tự hiển thị sư phạm của module trong lộ trình học.
     */
    @Column(nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EModuleStatus status = EModuleStatus.DRAFT;

    /**
     * Điểm đỗ mặc định (Fallback) bài thi vượt ải nếu chưa cấu hình ngưỡng theo AIM (mẫu 65/100).
     */
    @Column(nullable = false)
    private Integer defaultPassScore = 65;

    /**
     * Tỷ lệ bài giảng hoàn thành tối thiểu (%) để mở bài thi vượt ải module (mẫu 80%).
     */
    @Column(name = "required_completion_rate", nullable = false)
    private Integer requiredCompletionRate = 80;

    /**
     * Mô tả mục tiêu bài học của Module.
     */
    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Module thuộc về Part nào trong bài thi TOEIC.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = true)
    private Part part;

    /**
     * Module phụ trách rèn luyện một hoặc nhiều Năng lực (Ability).
     */
    @OneToMany(
            mappedBy = "module",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ModuleAbility> moduleAbilities = new ArrayList<>();

    /**
     * Danh sách bài học (Lesson) thuộc Module.
     * Sắp xếp tăng dần theo orderNo và cấu hình BatchSize để Hibernate tự động gom nạp IN (...) triệt tiêu N+1.
     */
    @OneToMany(
            mappedBy = "module",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("orderNo ASC")
    @BatchSize(size = 50)
    private List<Lesson> lessons = new ArrayList<>();

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
