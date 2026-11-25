package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.enums.diagnostic.EAttemptStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAttemptRepository extends JpaRepository<DiagnosticAttempt, Long> {

    List<DiagnosticAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<DiagnosticAttempt> findTopByUserIdOrderByStartedAtDesc(Long userId);

    /**
     * Lấy lượt thi chẩn đoán đã hoàn thành gần nhất của học viên (phục vụ BR02 khi đổi AIM).
     */
    Optional<DiagnosticAttempt> findTopByUserIdAndStatusOrderByCompletedAtDesc(Long userId, EAttemptStatus status);

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

    long countByStatus(EAttemptStatus status);

    long countByStatusAndCompletedAtBetween(EAttemptStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT d FROM DiagnosticAttempt d " +
           "JOIN FETCH d.user u " +
           "JOIN FETCH d.diagnosticTest t " +
           "WHERE d.status = :status " +
           "ORDER BY d.completedAt DESC")
    List<DiagnosticAttempt> findTopRecentAttempts(@Param("status") EAttemptStatus status, Pageable pageable);
}
