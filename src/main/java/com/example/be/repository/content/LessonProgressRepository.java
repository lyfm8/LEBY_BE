package com.example.be.repository.content;

import com.example.be.entity.content.LessonProgress;
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
 * Kho lưu trữ dữ liệu tiến độ học từng bài giảng của học viên (UC11).
 * Tối ưu truy vấn gom nhóm GROUP BY và IN (...) để triệt tiêu hoàn toàn lỗi N+1 Query.
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    /**
     * Tìm bản ghi tiến độ học của một học viên đối với một bài học cụ thể.
     *
     * @param userId   ID học viên
     * @param lessonId ID bài học
     * @return Optional chứa LessonProgress nếu học viên đã từng xem bài học này
     */
    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    /**
     * Nạp toàn bộ bản ghi tiến độ của một học viên cho danh sách bài học trong 1 câu SQL (IN).
     * Phục vụ màn hình Chi tiết Module (UC10) và thanh danh sách bài học phòng học Video (UC11).
     *
     * @param userId    ID học viên
     * @param lessonIds Tập hợp ID các bài học cần tra cứu
     * @return Danh sách bản ghi LessonProgress hiện có
     */
    List<LessonProgress> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

    /**
     * Đếm số bài học đã xuất bản (PUBLISHED) được hoàn thành bởi học viên theo từng Module
     * trong 1 câu GROUP BY duy nhất nhằm chống N+1 Query tại màn hình Lộ trình (UC09).
     *
     * @param userId       ID học viên
     * @param moduleIds    Tập hợp ID các Module trên lộ trình
     * @param lessonStatus Trạng thái bài học cần lọc (EModuleStatus.PUBLISHED)
     * @return Danh sách cặp [moduleId, completedLessonCount]
     */
    @Query("SELECT l.module.id, COUNT(lp.id) FROM LessonProgress lp " +
           "JOIN lp.lesson l " +
           "WHERE lp.user.id = :userId " +
           "AND lp.isCompleted = true " +
           "AND l.status = :lessonStatus " +
           "AND l.module.id IN :moduleIds " +
           "GROUP BY l.module.id")
    List<Object[]> countCompletedLessonsGroupedByModuleId(
            @Param("userId") Long userId,
            @Param("moduleIds") Collection<Long> moduleIds,
            @Param("lessonStatus") EModuleStatus lessonStatus
    );

    /**
     * Đếm số lượng bài học thuộc một loại cụ thể (ví dụ ELessonType.VIDEO) đã hoàn thành trong Module
     * để tính lại tỷ lệ hoàn thành phục vụ quy tắc mở khóa bài thi vượt ải BR03.
     *
     * @param userId       ID học viên
     * @param moduleId     ID của Module
     * @param lessonStatus Trạng thái xuất bản của bài học (PUBLISHED)
     * @param lessonType   Loại bài học cần đếm (VIDEO)
     * @return Số lượng bài giảng đã hoàn thành
     */
    @Query("SELECT COUNT(lp.id) FROM LessonProgress lp " +
           "JOIN lp.lesson l " +
           "WHERE lp.user.id = :userId " +
           "AND lp.isCompleted = true " +
           "AND l.module.id = :moduleId " +
           "AND l.status = :lessonStatus " +
           "AND l.lessonType = :lessonType")
    long countCompletedLessonsByUserAndModuleAndType(
            @Param("userId") Long userId,
            @Param("moduleId") Long moduleId,
            @Param("lessonStatus") EModuleStatus lessonStatus,
            @Param("lessonType") ELessonType lessonType
    );
}
