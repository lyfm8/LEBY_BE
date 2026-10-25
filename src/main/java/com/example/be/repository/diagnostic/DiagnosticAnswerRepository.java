package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAnswerRepository extends JpaRepository<DiagnosticAnswer, Long> {

    List<DiagnosticAnswer> findByDiagnosticAttemptId(Long diagnosticAttemptId);

    Optional<DiagnosticAnswer> findByDiagnosticAttemptIdAndQuestionId(Long diagnosticAttemptId, Long questionId);

    List<DiagnosticAnswer> findByDiagnosticAttemptIdAndIsCorrectFalse(Long diagnosticAttemptId);

    long countByDiagnosticAttemptIdAndIsCorrectTrue(Long diagnosticAttemptId);
}
