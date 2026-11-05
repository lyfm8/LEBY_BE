package com.example.be.repository.content;

import com.example.be.entity.content.LessonScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý các bản ghi phụ đề / lời thoại (Script) của bài nghe.
 */
@Repository
public interface LessonScriptRepository extends JpaRepository<LessonScript, Long> {

    /**
     * Lấy toàn bộ danh sách phụ đề của một bài học theo thứ tự thời gian.
     *
     * @param lessonId ID của bài học
     * @return Danh sách kịch bản phụ đề sắp xếp theo orderNo
     */
    List<LessonScript> findByLessonIdOrderByOrderNoAsc(Long lessonId);

    /**
     * Xóa toàn bộ kịch bản phụ đề của một bài học để phục vụ việc cập nhật hàng loạt.
     *
     * @param lessonId ID của bài học
     */
    void deleteByLessonId(Long lessonId);
}

