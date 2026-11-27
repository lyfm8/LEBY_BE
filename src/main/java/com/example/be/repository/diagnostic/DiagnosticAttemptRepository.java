package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.enums.diagnostic.EAttemptStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAttemptRepository extends JpaRepository<DiagnosticAttempt, Long> {

    List<DiagnosticAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<DiagnosticAttempt> findTopByUserIdOrderByStartedAtDesc(Long userId);

    /**
     * Lấy lượt thi chẩn đoán đã hoàn thành gần nhất của học viên (phục vụ BR02 khi đổi AIM & UC07 Luồng 1a).
     */
    Optional<DiagnosticAttempt> findTopByUserIdAndStatusOrderByCompletedAtDesc(Long userId, EAttemptStatus status);

    /**
     * Lấy lượt thi gần nhất theo userId và trạng thái bất kỳ (sắp xếp theo startedAt mới nhất — dùng cho /resume).
     */
    @EntityGraph(attributePaths = {"diagnosticTest", "targetProfile"})
    Optional<DiagnosticAttempt> findTopByUserIdAndStatusOrderByStartedAtDesc(Long userId, EAttemptStatus status);

    /**
     * Kiểm tra lượt thi đã hoàn thành theo đúng mục tiêu AIM cụ thể (phục vụ BR02 cho phép thi lại khi đổi AIM).
     */
    Optional<DiagnosticAttempt> findTopByUserIdAndTargetProfileIdAndStatusOrderByCompletedAtDesc(
            Long userId,
            Long targetProfileId,
            EAttemptStatus status
    );

    /**
     * Kiểm tra học viên đã hoàn thành ít nhất một lượt thi chẩn đoán nào chưa (không phân biệt đề hay AIM).
     */
    boolean existsByUserIdAndStatusIn(Long userId, Collection<EAttemptStatus> statuses);

    /**
     * Kiểm tra học viên đã từng mở xem đáp án chi tiết (Review) của bài thi chẩn đoán hay chưa.
     */
    boolean existsByUserIdAndHasViewedReviewTrue(Long userId);

    /**
     * Tìm phiên làm bài đang diễn ra (IN_PROGRESS) cho cùng đề thi và mục tiêu AIM nhằm tránh tạo trùng lượt thi.
     */
    Optional<DiagnosticAttempt> findTopByUserIdAndDiagnosticTestIdAndTargetProfileIdAndStatusOrderByStartedAtDesc(
            Long userId,
            Long diagnosticTestId,
            Long targetProfileId,
            EAttemptStatus status
    );

    /**
     * Tìm phiên làm bài đang diễn ra (IN_PROGRESS) khi targetProfile chưa được gán.
     */
    Optional<DiagnosticAttempt> findTopByUserIdAndDiagnosticTestIdAndStatusOrderByStartedAtDesc(
            Long userId,
            Long diagnosticTestId,
            EAttemptStatus status
    );

    /**
     * Nạp lượt thi kèm Đề thi, Người dùng và Mục tiêu AIM để tránh N+1 Query khi mở phòng thi hoặc xem báo cáo.
     */
    @Query("SELECT d FROM DiagnosticAttempt d " +
           "JOIN FETCH d.user u " +
           "JOIN FETCH d.diagnosticTest t " +
           "LEFT JOIN FETCH d.targetProfile tp " +
           "WHERE d.id = :attemptId")
    Optional<DiagnosticAttempt> findByIdWithContext(@Param("attemptId") Long attemptId);

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

    /**
     * Kiểm tra học viên có lượt thi chẩn đoán hoặc thi thử đang diễn ra (IN_PROGRESS) hay không.
     */
    boolean existsByUserIdAndStatus(Long userId, EAttemptStatus status);

    /**
     * Lấy danh sách toàn bộ lượt thi đang IN_PROGRESS của người dùng để tự động hủy (ABANDONED) khi khóa tài khoản.
     */
    List<DiagnosticAttempt> findAllByUserIdAndStatus(Long userId, EAttemptStatus status);

    /**
     * Truy vấn lịch sử các lượt làm bài chẩn đoán và thi thử của học viên kèm đề thi và AIM (tránh N+1).
     */
    @Query("SELECT d FROM DiagnosticAttempt d " +
           "JOIN FETCH d.diagnosticTest t " +
           "LEFT JOIN FETCH d.targetProfile tp " +
           "WHERE d.user.id = :userId " +
           "ORDER BY COALESCE(d.startedAt, d.createdAt) DESC")
    List<DiagnosticAttempt> findAllByUserIdWithContextOrderByRecent(@Param("userId") Long userId);
}
