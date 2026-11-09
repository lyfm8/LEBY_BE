package com.example.be.repository.question;

import com.example.be.entity.question.QuestionMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Kho lưu trữ dữ liệu cho bảng liên kết QuestionMedia.
 */
@Repository
public interface QuestionMediaRepository extends JpaRepository<QuestionMedia, Long> {

    /**
     * Lấy danh sách liên kết media của một câu hỏi theo thứ tự hiển thị.
     *
     * @param questionId ID của câu hỏi
     * @return Danh sách các bản ghi QuestionMedia
     */
    List<QuestionMedia> findByQuestionIdOrderByOrderNoAsc(Long questionId);

    /**
     * Xóa toàn bộ liên kết media của một câu hỏi.
     *
     * @param questionId ID của câu hỏi
     */
    void deleteByQuestionId(Long questionId);
}

