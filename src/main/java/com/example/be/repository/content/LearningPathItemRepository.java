package com.example.be.repository.content;

import com.example.be.entity.content.LearningPathItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý các mục mô-đun trong lộ trình học của học viên.
 */
@Repository
public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Long> {

    List<LearningPathItem> findByLearningPathIdOrderByOrderNoAsc(Long learningPathId);

    /**
     * Kiểm tra xem Module có đang được gán vào bất kỳ lộ trình học nào của học viên hay không.
     * Sử dụng để ngăn chặn xóa Module làm đứt gãy tiến trình học (Luồng 2c).
     *
     * @param moduleId ID của module cần kiểm tra
     * @return true nếu có ít nhất 1 học viên đang có module này trong lộ trình
     */
    boolean existsByModuleId(Long moduleId);
}
