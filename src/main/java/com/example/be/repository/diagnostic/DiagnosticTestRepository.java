package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.enums.diagnostic.ETestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTest, Long> {

    List<DiagnosticTest> findByStatusTrue();

    List<DiagnosticTest> findByTestTypeAndStatusTrue(ETestType testType);
}
