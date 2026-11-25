package com.example.be.repository.content;

import com.example.be.entity.content.LearningPath;
import com.example.be.enums.content.ELearningPathStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    List<LearningPath> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Truy vấn toàn bộ lịch sử lộ trình của học viên kèm TargetProfile (tránh N+1 query).
     * Dùng để tính version tiếp theo và xác định mục tiêu AIM trước đó khi tái sinh lộ trình.
     */
    @EntityGraph(attributePaths = {"targetProfile"})
    List<LearningPath> findWithTargetProfileByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LearningPath> findByUserIdAndStatus(Long userId, ELearningPathStatus status);

    List<LearningPath> findAllByUserIdAndStatus(Long userId, ELearningPathStatus status);

    long countByStatus(ELearningPathStatus status);
}
