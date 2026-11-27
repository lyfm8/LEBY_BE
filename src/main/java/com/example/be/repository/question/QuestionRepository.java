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

    /**
     * Lấy toàn bộ câu hỏi có thể làm bài kèm thông tin Part và Ability trong một truy vấn duy nhất (tránh N+1).
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.questionAbilities qa " +
           "LEFT JOIN FETCH qa.ability a " +
           "WHERE (q.parent IS NOT NULL OR NOT EXISTS (SELECT 1 FROM Question child WHERE child.parent = q))")
    List<Question> findAllPlayableWithAbilitiesAndPart();

    /**
     * Load câu hỏi theo danh sách ID kèm Ability để tránh N+1 khi chấm điểm Tầng 2.
     *
     * @param ids Danh sách ID câu hỏi cần load
     * @return Danh sách Question đã nạp sẵn questionAbilities và part
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.questionAbilities qa " +
           "LEFT JOIN FETCH qa.ability a " +
           "LEFT JOIN FETCH q.part p " +
           "WHERE q.id IN :ids")
    List<Question> findAllWithAbilitiesByIdIn(@Param("ids") List<Long> ids);

    /**
     * Load câu hỏi kèm Part, câu hỏi cha (Parent) và Media để render phòng thi không bị N+1 Query.
     * Lưu ý: Không JOIN FETCH cùng lúc 2 collection List để tránh MultipleBagFetchException của Hibernate.
     *
     * @param ids Danh sách ID câu hỏi cần load
     * @return Danh sách Question kèm Part, Parent và QuestionMedias
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.parent parent " +
           "LEFT JOIN FETCH parent.part pp " +
           "LEFT JOIN FETCH q.questionMedias qm " +
           "LEFT JOIN FETCH qm.media m " +
           "WHERE q.id IN :ids")
    List<Question> findAllWithMediasAndParentByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Load danh sách câu hỏi cha kèm tệp Media dùng chung (audio Part 3/4 hoặc ảnh bài đọc Part 6/7).
     *
     * @param parentIds Danh sách ID câu hỏi cha
     * @return Danh sách Question cha đã nạp sẵn questionMedias và media
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "LEFT JOIN FETCH q.questionMedias qm " +
           "LEFT JOIN FETCH qm.media m " +
           "WHERE q.id IN :parentIds")
    List<Question> findAllParentsWithMediasByIdIn(@Param("parentIds") Collection<Long> parentIds);

    /**
     * Đếm số câu hỏi có thể làm bài (câu đơn hoặc câu con trong chùm) gắn với tập Ability chỉ định.
     * Dùng để kiểm tra tính khả dụng của kho câu hỏi theo năng lực mô-đun (UC A07 - Luồng 6a, 6c).
     *
     * @param abilityIds Tập ID các Ability thuộc mô-đun
     * @return Tổng số câu hỏi playable khớp Ability
     */
    @Query("SELECT COUNT(DISTINCT q.id) FROM Question q " +
           "JOIN q.questionAbilities qa " +
           "WHERE qa.ability.id IN :abilityIds " +
           "AND (q.parent IS NOT NULL OR NOT EXISTS (SELECT 1 FROM Question child WHERE child.parent = q))")
    long countPlayableQuestionsByAbilityIds(@Param("abilityIds") Collection<Long> abilityIds);

    /**
     * Lấy danh sách câu hỏi có thể làm bài (kèm Part và Parent) gắn với tập Ability chỉ định.
     * Phục vụ chức năng tự động chọn câu hỏi ngẫu nhiên cho Bài thi vượt ải mô-đun (UC A07 - Luồng 6a).
     *
     * @param abilityIds Tập ID các Ability thuộc mô-đun
     * @return Danh sách Question playable đã nạp sẵn part và parent
     */
    @Query("SELECT DISTINCT q FROM Question q " +
           "JOIN q.questionAbilities qa " +
           "LEFT JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.parent parent " +
           "WHERE qa.ability.id IN :abilityIds " +
           "AND (q.parent IS NOT NULL OR NOT EXISTS (SELECT 1 FROM Question child WHERE child.parent = q))")
    List<Question> findPlayableQuestionsByAbilityIds(@Param("abilityIds") Collection<Long> abilityIds);
}
