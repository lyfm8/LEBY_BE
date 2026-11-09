package com.example.be.repository.question;

import com.example.be.entity.question.Question;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ dữ liệu cho thực thể Question.
 * Hỗ trợ truy vấn phân trang linh hoạt qua JpaSpecificationExecutor.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    /**
     * Đếm số lượng câu hỏi thuộc về một Part cụ thể.
     *
     * @param partId ID của Part
     * @return Số lượng câu hỏi
     */
    long countByPartId(Long partId);

    /**
     * Tìm chi tiết câu hỏi kèm nạp sẵn quan hệ Part để tối ưu hiệu năng.
     *
     * @param id ID của câu hỏi
     * @return Optional chứa Question nếu tìm thấy
     */
    @EntityGraph(attributePaths = {"part"})
    Optional<Question> findWithPartById(Long id);

    /**
     * Lấy danh sách câu hỏi con thuộc về danh sách câu hỏi cha chỉ định.
     *
     * @param parentIds Danh sách ID câu hỏi cha
     * @return Danh sách các câu hỏi con
     */
    List<Question> findByParentIdInOrderByOrderNoAsc(Collection<Long> parentIds);

    /**
     * Lấy toàn bộ ID của các câu hỏi con trực thuộc câu hỏi cha.
     *
     * @param parentId ID câu hỏi cha
     * @return Danh sách ID câu hỏi con
     */
    @Query("SELECT q.id FROM Question q WHERE q.parent.id = :parentId")
    List<Long> findSubQuestionIdsByParentId(@Param("parentId") Long parentId);
}
