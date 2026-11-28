package com.example.be.repository.content;

import com.example.be.entity.content.Lesson;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Đếm tổng số bài học cho nhiều Module trong 1 query duy nhất nhằm tránh N+1 query.
     * Trả về [moduleId, totalLessons].
     */
    @Query("SELECT l.module.id, COUNT(l.id) FROM Lesson l WHERE l.module.id IN :moduleIds GROUP BY l.module.id")
    List<Object[]> countByModuleIdIn(@Param("moduleIds") Collection<Long> moduleIds);

    /**
     * Đếm tổng số bài học theo trạng thái xuất bản (chỉ đếm PUBLISHED cho học viên) cho nhiều Module
     * trong 1 câu GROUP BY duy nhất nhằm triệt tiêu lỗi N+1 Query tại màn hình Lộ trình (UC09).
     *
     * @param moduleIds Tập hợp ID các Module cần thống kê
     * @param status    Trạng thái bài học cần lọc (thường là EModuleStatus.PUBLISHED)
     * @return Danh sách cặp [moduleId, publishedLessonCount]
     */
    @Query("SELECT l.module.id, COUNT(l.id) FROM Lesson l " +
           "WHERE l.module.id IN :moduleIds AND l.status = :status " +
           "GROUP BY l.module.id")
    List<Object[]> countByModuleIdInAndStatus(
            @Param("moduleIds") Collection<Long> moduleIds,
            @Param("status") EModuleStatus status
    );

    /**
     * Nạp danh sách bài học đã xuất bản của một Module kèm sẵn Ability trong 1 câu SQL
     * nhằm triệt tiêu N+1 Query tại màn hình Chi tiết Module (UC10).
     *
     * @param moduleId ID của Module cần nạp bài học
     * @param status   Trạng thái bài học cần lọc (EModuleStatus.PUBLISHED)
     * @return Danh sách bài học sắp xếp tăng dần theo thứ tự hiển thị orderNo
     */
    @Query("SELECT l FROM Lesson l " +
           "LEFT JOIN FETCH l.ability a " +
           "WHERE l.module.id = :moduleId AND l.status = :status " +
           "ORDER BY l.orderNo ASC")
    List<Lesson> findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(
            @Param("moduleId") Long moduleId,
            @Param("status") EModuleStatus status
    );

    /**
     * Đếm số lượng câu hỏi luyện tập / ôn tập gắn trong bảng practice_questions theo từng bài học
     * (áp dụng cho cả bài giảng VIDEO và bài thực hành PRACTICE theo quy chuẩn UC11-UC12) trong 1 câu GROUP BY.
     *
     * @param lessonIds Tập hợp ID các bài học cần đếm số câu hỏi
     * @return Danh sách cặp [lessonId, questionCount]
     */
    @Query("SELECT pq.lesson.id, COUNT(pq.id) FROM PracticeQuestion pq " +
           "WHERE pq.lesson.id IN :lessonIds " +
           "GROUP BY pq.lesson.id")
    List<Object[]> countPracticeQuestionsByLessonIdIn(@Param("lessonIds") Collection<Long> lessonIds);

    /**
     * Nạp chi tiết một bài học kèm sẵn Module, Part và Ability trong 1 câu SQL
     * để phục vụ phòng học bài giảng Video (UC11) mà không phát sinh N+1 Query.
     *
     * @param lessonId ID bài học cần nạp
     * @return Optional chứa Lesson kèm ngữ cảnh Module/Part/Ability
     */
    @Query("SELECT l FROM Lesson l " +
           "JOIN FETCH l.module m " +
           "LEFT JOIN FETCH m.part p " +
           "LEFT JOIN FETCH l.ability a " +
           "WHERE l.id = :lessonId")
    Optional<Lesson> findByIdWithModulePartAndAbility(@Param("lessonId") Long lessonId);

    /**
     * Nạp hàng loạt các tệp Media gắn trên danh sách câu hỏi (bao gồm cả câu hỏi cha parent)
     * trong 1 câu SQL duy nhất nhằm triệt tiêu lỗi N+1 Query và giữ Zero Merge Conflict với package question.
     *
     * @param questionIds Tập hợp ID câu hỏi cần nạp media
     * @return Danh sách QuestionMedia đã nạp sẵn Media theo thứ tự orderNo tăng dần
     */
    @Query("SELECT qm FROM QuestionMedia qm " +
           "JOIN FETCH qm.media m " +
           "WHERE qm.question.id IN :questionIds " +
           "ORDER BY qm.orderNo ASC")
    List<QuestionMedia> findQuestionMediasByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);

    /**
     * Đếm tổng số bài học theo trạng thái xuất bản và loại bài học (ví dụ ELessonType.VIDEO) cho nhiều Module
     * trong đúng 1 câu SQL GROUP BY để tính chính xác tỷ lệ kế thừa tiến độ khi đổi mục tiêu AIM (0 N+1 Query).
     *
     * @param moduleIds  Tập hợp ID các Module cần thống kê
     * @param status     Trạng thái bài học cần lọc (EModuleStatus.PUBLISHED)
     * @param lessonType Loại bài học cần lọc (ELessonType.VIDEO)
     * @return Danh sách cặp [moduleId, lessonCount]
     */
    @Query("SELECT l.module.id, COUNT(l.id) FROM Lesson l " +
           "WHERE l.module.id IN :moduleIds AND l.status = :status AND l.lessonType = :lessonType " +
           "GROUP BY l.module.id")
    List<Object[]> countByModuleIdInAndStatusAndLessonType(
            @Param("moduleIds") Collection<Long> moduleIds,
            @Param("status") EModuleStatus status,
            @Param("lessonType") ELessonType lessonType
    );

    /**
     * Nạp toàn bộ đề cương bài học đã xuất bản (PUBLISHED) cho nhiều Module trong đúng 1 câu SQL (IN)
     * phục vụ Thư viện khóa học 7 Part (UC17 Catalog) nhằm triệt tiêu hoàn toàn N+1 Query.
     *
     * @param moduleIds Tập hợp ID các Module cần nạp đề cương bài học
     * @param status    Trạng thái xuất bản cần lọc (EModuleStatus.PUBLISHED)
     * @return Danh sách bài học sắp xếp theo moduleId và thứ tự orderNo tăng dần
     */
    @Query("SELECT l FROM Lesson l " +
           "WHERE l.module.id IN :moduleIds AND l.status = :status " +
           "ORDER BY l.module.id ASC, l.orderNo ASC")
    List<Lesson> findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(
            @Param("moduleIds") Collection<Long> moduleIds,
            @Param("status") EModuleStatus status
    );
}

