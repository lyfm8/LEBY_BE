package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.ELearningPathStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lộ trình học tập cá nhân hóa được sinh ra cho User.
 * Áp dụng Kịch bản B: Gộp trực tiếp TargetProfile và DiagnosticAttempt vào LearningPath
 * để tối ưu hiệu năng, giảm 1 bảng trung gian và lưu vết trọn vẹn lịch sử lộ trình.
 */
@Entity
@Table(name = "learning_paths")
@Getter
@Setter
@NoArgsConstructor
public class LearningPath extends AbstractEntity {

    /**
     * Phiên bản lộ trình (Int).
     */
    private Integer version;

    /**
     * Trạng thái lộ trình: ACTIVE (đang học), ARCHIVED (đã lưu trữ khi đổi AIM), COMPLETED (đã hoàn thành).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ELearningPathStatus status = ELearningPathStatus.ACTIVE;

    /**
     * Học viên sở hữu lộ trình học tập này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Mục tiêu điểm TOEIC (AIM) mà lộ trình này hướng tới.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;

    /**
     * Lượt thi chẩn đoán (DiagnosticAttempt) đầu vào làm căn cứ sinh ra lộ trình này.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_attempt_id", nullable = true)
    private DiagnosticAttempt diagnosticAttempt;

    /**
     * Danh sách các module trong lộ trình học.
     */
    @OneToMany(
            mappedBy = "learningPath",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LearningPathItem> items = new ArrayList<>();
}
