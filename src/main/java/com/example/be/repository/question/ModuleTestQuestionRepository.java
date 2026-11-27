package com.example.be.repository.question;

import com.example.be.entity.question.ModuleTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Kho lưu trữ dữ liệu liên kết giữa Bài kiểm tra Module và Câu hỏi.
 */
@Repository
public interface ModuleTestQuestionRepository extends JpaRepository<ModuleTestQuestion, Long> {

    /**
     * Kiểm tra xem bất kỳ câu hỏi nào trong tập hợp có đang nằm trong bài thi module nào không.
     *
     * @param questionIds Tập hợp ID các câu hỏi cần kiểm tra
     * @return true nếu tồn tại ít nhất 1 câu hỏi đang được sử dụng trong bài thi module
     */
    boolean existsByQuestionIdIn(Collection<Long> questionIds);

    /**
     * Lấy danh sách câu hỏi bài thi vượt ải của một Module kèm Question, Part và Parent (triệt tiêu N+1).
     *
     * @param moduleId ID của Module cần lấy danh sách câu hỏi
     * @return Danh sách ModuleTestQuestion sắp xếp theo orderNo tăng dần
     */
    @Query("SELECT mtq FROM ModuleTestQuestion mtq " +
           "JOIN FETCH mtq.question q " +
           "LEFT JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.parent parent " +
           "WHERE mtq.module.id = :moduleId " +
           "ORDER BY mtq.orderNo ASC")
    List<ModuleTestQuestion> findByModuleIdWithQuestionOrderByOrderNoAsc(@Param("moduleId") Long moduleId);

    /**
     * Đếm số lượng câu hỏi bài thi vượt ải hiện có theo từng Module trong một câu truy vấn duy nhất.
     *
     * @param moduleIds Tập hợp ID các Module cần thống kê
     * @return Danh sách mảng [moduleId, questionCount]
     */
    @Query("SELECT mtq.module.id, COUNT(mtq.id) FROM ModuleTestQuestion mtq " +
           "WHERE mtq.module.id IN :moduleIds " +
           "GROUP BY mtq.module.id")
    List<Object[]> countQuestionsGroupedByModuleIds(@Param("moduleIds") Collection<Long> moduleIds);
}

