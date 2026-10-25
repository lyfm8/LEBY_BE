package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticAttemptRepository extends JpaRepository<DiagnosticAttempt, Long> {

    List<DiagnosticAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<DiagnosticAttempt> findTopByUserIdOrderByStartedAtDesc(Long userId);
}
