package com.example.be.repository.diagnostic;

import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTest, Long>, JpaSpecificationExecutor<DiagnosticTest> {

    /**
     * Tìm danh sách đề thi đang Active theo loại đề.
     */
    List<DiagnosticTest> findByTestTypeAndExamStatus(ETestType testType, EExamStatus examStatus);

    /**
     * Tìm đề chẩn đoán duy nhất đang Active.
     */
    Optional<DiagnosticTest> findFirstByTestTypeAndExamStatus(ETestType testType, EExamStatus examStatus);

    /**
     * Đếm tổng số đề theo loại (phục vụ số lượng trên Tab).
     */
    long countByTestType(ETestType testType);

    /**
     * Đếm tổng số đề theo trạng thái.
     */
    long countByExamStatus(EExamStatus examStatus);
}
