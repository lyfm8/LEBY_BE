package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAnswerRepository extends JpaRepository<DiagnosticAnswer, Long> {

    List<DiagnosticAnswer> findByDiagnosticAttemptId(Long diagnosticAttemptId);

    /**
     * Lấy toàn bộ đáp án của lượt thi kèm câu hỏi và Part (tránh N+1 Query khi dựng báo cáo UC07).
     *
     * @param diagnosticAttemptId ID lượt thi chẩn đoán
     * @return Danh sách câu trả lời
     */
    @Query("SELECT a FROM DiagnosticAnswer a " +
           "JOIN FETCH a.question q " +
           "LEFT JOIN FETCH q.part p " +
           "WHERE a.diagnosticAttempt.id = :attemptId")
    List<DiagnosticAnswer> findByDiagnosticAttemptIdWithQuestion(@Param("attemptId") Long diagnosticAttemptId);

    Optional<DiagnosticAnswer> findByDiagnosticAttemptIdAndQuestionId(Long diagnosticAttemptId, Long questionId);

    List<DiagnosticAnswer> findByDiagnosticAttemptIdAndIsCorrectFalse(Long diagnosticAttemptId);

    long countByDiagnosticAttemptIdAndIsCorrectTrue(Long diagnosticAttemptId);
}
