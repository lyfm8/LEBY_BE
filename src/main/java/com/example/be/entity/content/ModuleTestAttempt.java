package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.user.User;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestResult;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho một lần Student thực hiện ModuleTest.
 * Rule 23: ModuleTestAttempt chứa đủ thông tin kết quả, không cần thêm ModuleTestResult.
 * Rule 40: Không duplicate Entity.
 * Rule 36: score + ModuleTest.passScore đủ để Service kiểm tra unlock condition.
 */
@Entity
@Table(name = "module_test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTestAttempt extends AbstractEntity {

    /**
     * score: Int - điểm đạt được trong lần attempt này
     */
    private Integer score;

    @Enumerated(EnumType.STRING)
    private ETestResult result;

    /**
     * Trạng thái phiên làm bài vượt ải: IN_PROGRESS, SUBMITTED, TIMED_OUT, ABANDONED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EAttemptStatus status = EAttemptStatus.IN_PROGRESS;

    /**
     * isSubmitted: Boolean - attempt này đã submit chưa
     */
    @Column(nullable = false)
    private Boolean isSubmitted = false;

    /**
     * Thời điểm bắt đầu làm bài thi vượt ải.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Thời điểm nộp bài.
     */
    private LocalDateTime submittedAt;

    /**
     * Số giây làm bài còn lại được đồng bộ gần nhất lên máy chủ (UC13 Luồng 7a).
     */
    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    /**
     * Tổng thời gian làm bài thực tế tính bằng giây (phục vụ báo cáo UC14).
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * Snapshot JSON lưu thứ tự câu hỏi và bảng ánh xạ xáo trộn phương án A-B-C-D cho lượt thi (BR05).
     */
    @Column(name = "question_snapshot", columnDefinition = "JSON")
    private String questionSnapshot;

    @OneToMany(
            mappedBy = "moduleTestAttempt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ModuleTestAnswer> answers = new ArrayList<>();

    /**
     * ModuleTestAttempt * ---- 1 Module.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * ModuleTestAttempt * ---- 1 User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
