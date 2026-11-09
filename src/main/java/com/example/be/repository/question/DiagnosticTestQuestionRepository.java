package com.example.be.repository.question;

import com.example.be.entity.question.DiagnosticTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * Kho lưu trữ dữ liệu liên kết giữa Đề thi chẩn đoán và Câu hỏi.
 */
@Repository
public interface DiagnosticTestQuestionRepository extends JpaRepository<DiagnosticTestQuestion, Long> {

    /**
     * Kiểm tra xem bất kỳ câu hỏi nào trong tập hợp có đang nằm trong đề thi chẩn đoán nào không.
     *
     * @param questionIds Tập hợp ID các câu hỏi cần kiểm tra
     * @return true nếu tồn tại ít nhất 1 câu hỏi đang được sử dụng trong đề thi
     */
    boolean existsByQuestionIdIn(Collection<Long> questionIds);
}

