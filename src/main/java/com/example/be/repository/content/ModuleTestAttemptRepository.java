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

    /**
     * Lấy danh sách moduleId và điểm số cao nhất mà học viên từng thi vượt ải thành công (BR06).
     *
     * @param userId ID học viên
     * @param passResult Trạng thái đạt chuẩn (ETestResult.PASS)
     * @return Danh sách cặp [moduleId, maxScore]
     */
    @Query("SELECT a.module.id, MAX(a.score) FROM ModuleTestAttempt a " +
           "WHERE a.user.id = :userId AND a.isSubmitted = true AND a.result = :passResult " +
           "GROUP BY a.module.id")
    List<Object[]> findPassedModuleMaxScoresByUserId(@Param("userId") Long userId, @Param("passResult") ETestResult passResult);

    /**
     * Kiểm tra học viên có lượt thi vượt ải mô-đun đang làm dở (chưa nộp) hay không.
     */
    boolean existsByUserIdAndIsSubmittedFalse(Long userId);

    /**
     * Lấy lịch sử thi vượt ải mô-đun của một học viên kèm Module và Part (tránh N+1 query).
     */
    @Query("SELECT a FROM ModuleTestAttempt a " +
           "JOIN FETCH a.module m " +
           "LEFT JOIN FETCH m.part p " +
           "WHERE a.user.id = :userId " +
           "ORDER BY COALESCE(a.submittedAt, a.createdAt) DESC")
    List<ModuleTestAttempt> findAllByUserIdWithModuleOrderByRecent(@Param("userId") Long userId);

    /**
     * Lấy danh sách các lượt thi vượt ải đã nộp của học viên trên một Module cụ thể,
     * sắp xếp mới nhất trước để xác định trạng thái đã đỗ, điểm cao nhất và lượt thi gần nhất (UC10 Luồng 6b).
     *
     * @param userId   ID học viên đang đăng nhập
     * @param moduleId ID của Module cần tra cứu lịch sử thi vượt ải
     * @return Danh sách các lượt thi đã nộp sắp xếp giảm dần theo thời điểm nộp bài
     */
    List<ModuleTestAttempt> findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(Long userId, Long moduleId);
}
