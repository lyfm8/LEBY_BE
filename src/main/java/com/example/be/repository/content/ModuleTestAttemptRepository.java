package com.example.be.repository.content;

import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.enums.diagnostic.ETestResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository thao tác với bảng lượt thi vượt ải module (module_test_attempts).
 * Phục vụ truy vấn dữ liệu thống kê, tỷ lệ đỗ và Top mô-đun khó cho Admin Dashboard.
 */
@Repository
public interface ModuleTestAttemptRepository extends JpaRepository<ModuleTestAttempt, Long> {

    long countByIsSubmittedTrue();

    long countByIsSubmittedTrueAndResult(ETestResult result);

    long countByIsSubmittedTrueAndSubmittedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByIsSubmittedTrueAndResultAndSubmittedAtBetween(ETestResult result, LocalDateTime start, LocalDateTime end);

    /**
     * Nạp kèm User và Module trong 1 câu SQL (JOIN FETCH) để triệt tiêu lỗi N+1 Query.
     */
    @Query("SELECT a FROM ModuleTestAttempt a " +
           "JOIN FETCH a.user u " +
           "JOIN FETCH a.module m " +
           "WHERE a.isSubmitted = true " +
           "ORDER BY a.submittedAt DESC")
    List<ModuleTestAttempt> findTopRecentAttempts(Pageable pageable);

    /**
     * Tính toán Top mô-đun khó có tỷ lệ thi trượt cao nhất trực tiếp trong Database (O(N) Pushdown).
     * BẮT BUỘC: Chỉ lọc những mô-đun thực sự có học viên thi trượt (failCount > 0) để tránh hiển thị mô-đun 0% trượt.
     */
    @Query("SELECT m.id, m.title, p.name, COUNT(a.id), " +
           "SUM(CASE WHEN a.result = :failResult THEN 1 ELSE 0 END) " +
           "FROM ModuleTestAttempt a " +
           "JOIN a.module m " +
           "LEFT JOIN m.part p " +
           "WHERE a.isSubmitted = true " +
           "GROUP BY m.id, m.title, p.name " +
           "HAVING SUM(CASE WHEN a.result = :failResult THEN 1 ELSE 0 END) > 0 " +
           "ORDER BY (SUM(CASE WHEN a.result = :failResult THEN 1.0 ELSE 0.0 END) / COUNT(a.id)) DESC")
    List<Object[]> findTopDifficultModules(@Param("failResult") ETestResult failResult, Pageable pageable);
}
