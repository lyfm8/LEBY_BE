package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.PartDiagnosticResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartDiagnosticResultRepository extends JpaRepository<PartDiagnosticResult, Long> {

    List<PartDiagnosticResult> findByDiagnosticAttemptId(Long diagnosticAttemptId);
}
