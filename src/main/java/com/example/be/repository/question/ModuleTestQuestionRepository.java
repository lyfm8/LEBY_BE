package com.example.be.repository.question;

import com.example.be.entity.question.ModuleTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

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
}

