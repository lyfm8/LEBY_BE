package com.example.be.features.diagnostic.entity;
import com.example.be.features.content.entity.Part;

import com.example.be.features.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.example.be.features.diagnostic.enums.ETestType;

/**
 * Lượt làm bài Diagnostic Attempt.
 * Lưu trữ thông tin một lần user làm bài test đánh giá năng lực.
 * Một lượt test tổng hợp (Tier 1) sẽ sinh ra nhiều PartDiagnosticResult.
 */
@Entity
@Table(name = "diagnostic_attempts")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * startedAt: Date - ngày bắt đầu làm diagnostic
     * Rule 18: Date → LocalDate.
     */
    private LocalDate startedAt;

    /**
     * completedAt: Date - ngày hoàn thành diagnostic
     * Rule 18: Date → LocalDate.
     */
    private LocalDate completedAt;

    /**
     * DiagnosticAttempt * ---- 1 User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Lượt làm bài này được thực hiện trên Đề thi (DiagnosticTest) gốc nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_test_id", nullable = false)
    private DiagnosticTest diagnosticTest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ETestType testType;

    /**
     * Kết quả của từng Part trong lượt làm bài này (chỉ có data nếu testType = TIER_1_PART).
     */
    @OneToMany(
            mappedBy = "diagnosticAttempt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PartDiagnosticResult> partResults = new ArrayList<>();
}
