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

    /**
     * Lấy danh sách câu hỏi có thể gán vào đề thi (câu đơn lập hoặc câu con trong chùm, không lấy câu cha chỉ chứa bài đọc).
     */
    @Query("SELECT q FROM Question q JOIN FETCH q.part p WHERE p.id = :partId AND (q.parent IS NOT NULL OR NOT EXISTS (SELECT 1 FROM Question child WHERE child.parent = q))")
    List<Question> findPlayableQuestionsByPartId(@Param("partId") Long partId);

    /**
     * Đếm số câu hỏi có thể làm bài theo ID của Part (1 đến 7).
     */
    @Query("SELECT COUNT(q) FROM Question q WHERE q.part.id = :partId AND (q.parent IS NOT NULL OR NOT EXISTS (SELECT 1 FROM Question child WHERE child.parent = q))")
    long countPlayableQuestionsByPartId(@Param("partId") Long partId);
}
