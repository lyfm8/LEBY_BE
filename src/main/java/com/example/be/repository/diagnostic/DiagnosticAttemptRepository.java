package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.enums.diagnostic.EAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAttemptRepository extends JpaRepository<DiagnosticAttempt, Long> {

    List<DiagnosticAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<DiagnosticAttempt> findTopByUserIdOrderByStartedAtDesc(Long userId);

    /**
     * Kiểm tra xem đề thi đã có lượt làm bài nào hay chưa (bất kỳ trạng thái nào).
     */
    boolean existsByDiagnosticTestId(Long testId);

    /**
     * Kiểm tra xem đề thi có lượt làm bài với trạng thái cụ thể hay không (ví dụ IN_PROGRESS).
     */
    boolean existsByDiagnosticTestIdAndStatus(Long testId, EAttemptStatus status);

    /**
     * Đếm tổng số lượt làm bài của một đề thi.
     */
    long countByDiagnosticTestId(Long testId);
}
