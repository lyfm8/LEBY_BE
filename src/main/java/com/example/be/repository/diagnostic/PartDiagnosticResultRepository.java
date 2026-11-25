package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.PartDiagnosticResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartDiagnosticResultRepository extends JpaRepository<PartDiagnosticResult, Long> {

    List<PartDiagnosticResult> findByDiagnosticAttemptId(Long diagnosticAttemptId);

    /**
     * Lấy danh sách kết quả từng Part kèm thông tin Part (tránh N+1 Query khi dựng báo cáo UC07).
     *
     * @param diagnosticAttemptId ID lượt thi chẩn đoán
     * @return Danh sách kết quả Part sắp xếp theo part.id tăng dần
     */
    @Query("SELECT pr FROM PartDiagnosticResult pr " +
           "JOIN FETCH pr.part p " +
           "WHERE pr.diagnosticAttempt.id = :attemptId " +
           "ORDER BY p.id ASC")
    List<PartDiagnosticResult> findByDiagnosticAttemptIdWithPart(@Param("attemptId") Long diagnosticAttemptId);
}
