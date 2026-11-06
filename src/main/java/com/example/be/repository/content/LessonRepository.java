package com.example.be.repository.content;

import com.example.be.entity.content.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu bài học (Lesson).
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Lấy toàn bộ danh sách bài học thuộc một Module, sắp xếp theo thứ tự hiển thị tăng dần.
     *
     * @param moduleId ID của module
     * @return Danh sách bài học sắp xếp theo orderNo
     */
    List<Lesson> findByModuleIdOrderByOrderNoAsc(Long moduleId);

    /**
     * Tìm orderNo lớn nhất hiện tại của các bài học trong một Module để tự động sinh số thứ tự tiếp theo.
     *
     * @param moduleId ID của module
     * @return Giá trị orderNo lớn nhất hiện tại
     */
    @Query("SELECT MAX(l.orderNo) FROM Lesson l WHERE l.module.id = :moduleId")
    Optional<Integer> findMaxOrderNoByModuleId(@Param("moduleId") Long moduleId);

    /**
     * Đếm tổng số bài học thuộc một Module.
     *
     * @param moduleId ID của module
     * @return Số lượng bài học
     */
    long countByModuleId(Long moduleId);
}

