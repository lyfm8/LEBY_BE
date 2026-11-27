package com.example.be.repository.content;

import com.example.be.entity.content.LearningPath;
import com.example.be.enums.content.ELearningPathStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Truy vấn các lộ trình hiện hành (ACTIVE hoặc COMPLETED) của học viên kèm sẵn TargetProfile
     * trong 1 câu SQL để tránh N+1 query và xử lý đúng trường hợp PASS 100% 7 Part (UC09 Luồng 4a).
     *
     * @param userId   ID học viên đang đăng nhập
     * @param statuses Tập hợp trạng thái hợp lệ (ACTIVE, COMPLETED)
     * @return Danh sách lộ trình sắp xếp theo phiên bản mới nhất trước
     */
    @EntityGraph(attributePaths = {"targetProfile"})
    List<LearningPath> findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(
            Long userId,
            Collection<ELearningPathStatus> statuses
    );

    Optional<LearningPath> findByUserIdAndStatus(Long userId, ELearningPathStatus status);

    List<LearningPath> findAllByUserIdAndStatus(Long userId, ELearningPathStatus status);

    long countByStatus(ELearningPathStatus status);
}
