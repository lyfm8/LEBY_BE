package com.example.be.repository.user;

import com.example.be.enums.user.ERole;
import com.example.be.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findUsersByFilters(@Param("keyword") String keyword, 
                                  @Param("role") ERole role, 
                                  @Param("isActive") Boolean isActive, 
                                  Pageable pageable);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    /**
     * Tăng tokenVersion thêm 1 trực tiếp trong DB mà không cần load entity lên.
     * Dùng khi logout hoặc đổi mật khẩu để vô hiệu hóa toàn bộ token cũ.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :id")
    void incrementTokenVersion(@Param("id") Long id);

    long countByTargetProfileId(Long targetProfileId);

    long countByRole(ERole role);

    long countByRoleAndCreatedAtBetween(ERole role, LocalDateTime start, LocalDateTime end);

    @Query("SELECT tp.targetTotalScore, COUNT(u.id) FROM User u " +
           "JOIN u.targetProfile tp " +
           "WHERE u.role = :role " +
           "GROUP BY tp.targetTotalScore " +
           "ORDER BY tp.targetTotalScore ASC")
    List<Object[]> countUsersByAimTarget(@Param("role") ERole role);

    /**
     * Nạp kèm targetProfile (Eager Join) để triệt tiêu lỗi N+1 Query khi ánh xạ nhật ký hoạt động học viên.
     */
    @EntityGraph(attributePaths = {"targetProfile"})
    List<User> findTop5ByRoleOrderByCreatedAtDesc(ERole role);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE u.role = :role AND (u.id IN (" +
           "  SELECT a.user.id FROM ModuleTestAttempt a WHERE a.submittedAt >= :since" +
           ") OR u.id IN (" +
           "  SELECT d.user.id FROM DiagnosticAttempt d WHERE d.completedAt >= :since" +
           "))")
    long countActiveStudentsSince(@Param("role") ERole role, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u WHERE u.role = :role AND (u.id IN (" +
           "  SELECT a.user.id FROM ModuleTestAttempt a WHERE a.submittedAt BETWEEN :start AND :end" +
           ") OR u.id IN (" +
           "  SELECT d.user.id FROM DiagnosticAttempt d WHERE d.completedAt BETWEEN :start AND :end" +
           "))")
    long countActiveStudentsBetween(@Param("role") ERole role, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
