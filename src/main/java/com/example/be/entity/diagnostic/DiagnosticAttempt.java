package com.example.be.entity.diagnostic;

import com.example.be.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lượt làm bài Diagnostic Attempt.
 * Lưu trữ thông tin một lần user làm bài Comprehensive Diagnostic Test.
 * Sau khi nộp bài, hệ thống tự chấm 7 Part và tính Ability, sinh ra PartDiagnosticResult.
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
     * startedAt: LocalDateTime - thời điểm bắt đầu làm bài (cần độ chính xác đến giây cho durationSeconds).
     */
    private LocalDateTime startedAt;

    /**
     * completedAt: LocalDateTime - thời điểm nộp bài.
     */
    private LocalDateTime completedAt;

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

    /**
     * Kết quả điểm của từng Part (7 bản ghi) sau khi bộ máy chấm điểm hoàn tất.
     */
    @OneToMany(
            mappedBy = "diagnosticAttempt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PartDiagnosticResult> partResults = new ArrayList<>();
}
