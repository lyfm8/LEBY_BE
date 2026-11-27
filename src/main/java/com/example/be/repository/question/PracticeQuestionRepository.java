package com.example.be.repository.question;

import com.example.be.entity.question.PracticeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Kho lưu trữ dữ liệu cho thực thể trung gian PracticeQuestion (gắn câu hỏi luyện tập vào bài học).
 */
@Repository
public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {

    /**
     * Lấy toàn bộ danh sách câu hỏi luyện tập của một bài học kèm nạp sẵn Question, Part và Parent
     * để triệt tiêu lỗi N+1 Query khi hiển thị Tab 3 của quản trị bài giảng.
     * Lưu ý: Không JOIN FETCH thêm collection List khác để tránh MultipleBagFetchException.
     *
     * @param lessonId ID của bài học cần lấy danh sách câu hỏi
     * @return Danh sách PracticeQuestion sắp xếp tăng dần theo thứ tự hiển thị orderNo
     */
    @Query("SELECT pq FROM PracticeQuestion pq " +
           "JOIN FETCH pq.question q " +
           "LEFT JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.parent parent " +
           "WHERE pq.lesson.id = :lessonId " +
           "ORDER BY pq.orderNo ASC")
    List<PracticeQuestion> findAllWithQuestionAndPartByLessonId(@Param("lessonId") Long lessonId);

    /**
     * Lấy danh sách PracticeQuestion thuần theo lessonId sắp xếp tăng dần theo orderNo
     * để phục vụ thao tác đồng bộ in-place diff trong cùng transaction.
     *
     * @param lessonId ID của bài học
     * @return Danh sách PracticeQuestion theo thứ tự
     */
    List<PracticeQuestion> findByLessonIdOrderByOrderNoAsc(Long lessonId);

    /**
     * Đếm tổng số câu hỏi luyện tập đã được gắn vào một bài học.
     *
     * @param lessonId ID của bài học
     * @return Số lượng câu hỏi luyện tập
     */
    long countByLessonId(Long lessonId);

    /**
     * Kiểm tra một câu hỏi cụ thể đã được gắn vào bài học hay chưa.
     *
     * @param lessonId   ID của bài học
     * @param questionId ID của câu hỏi trong Ngân hàng câu hỏi
     * @return true nếu đã tồn tại liên kết, false nếu chưa
     */
    boolean existsByLessonIdAndQuestionId(Long lessonId, Long questionId);

    /**
     * Tìm số thứ tự orderNo lớn nhất hiện tại của các câu hỏi trong một bài học.
     *
     * @param lessonId ID của bài học
     * @return Optional chứa giá trị orderNo lớn nhất nếu bài học đã có câu hỏi
     */
    @Query("SELECT MAX(pq.orderNo) FROM PracticeQuestion pq WHERE pq.lesson.id = :lessonId")
    Optional<Integer> findMaxOrderNoByLessonId(@Param("lessonId") Long lessonId);
}
