package com.example.be.repository.content;

import com.example.be.entity.content.LearningPathItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý các mục mô-đun trong lộ trình học của học viên.
 */
@Repository
public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Long> {

    List<LearningPathItem> findByLearningPathIdOrderByOrderNoAsc(Long learningPathId);

    /**
     * Nạp danh sách chặng học của lộ trình kèm sẵn Module, Part và Ability trong 1 query để triệt tiêu N+1.
     */
    @Query("SELECT i FROM LearningPathItem i " +
           "JOIN FETCH i.module m " +
           "LEFT JOIN FETCH m.part p " +
           "LEFT JOIN FETCH i.ability a " +
           "WHERE i.learningPath.id = :learningPathId " +
           "ORDER BY i.orderNo ASC")
    List<LearningPathItem> findByLearningPathIdWithDetailsOrderByOrderNoAsc(@Param("learningPathId") Long learningPathId);

    /**
     * Kiểm tra xem Module có đang được gán vào bất kỳ lộ trình học nào của học viên hay không.
     * Sử dụng để ngăn chặn xóa Module làm đứt gãy tiến trình học (Luồng 2c).
     *
     * @param moduleId ID của module cần kiểm tra
     * @return true nếu có ít nhất 1 học viên đang có module này trong lộ trình
     */
    boolean existsByModuleId(Long moduleId);
}
