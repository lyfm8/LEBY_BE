package com.example.be.repository.question;

import com.example.be.entity.question.DiagnosticTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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

    /**
     * Lấy danh sách câu hỏi thuộc đề thi theo thứ tự tăng dần của orderNo.
     *
     * @param diagnosticTestId ID đề thi chẩn đoán
     * @return Danh sách DiagnosticTestQuestion đã sắp xếp
     */
    @Query("SELECT dtq FROM DiagnosticTestQuestion dtq " +
           "JOIN FETCH dtq.question q " +
           "LEFT JOIN FETCH q.part p " +
           "LEFT JOIN FETCH q.parent parent " +
           "WHERE dtq.diagnosticTest.id = :diagnosticTestId " +
           "ORDER BY dtq.orderNo ASC")
    List<DiagnosticTestQuestion> findAllByDiagnosticTestIdOrderByOrderNoAsc(
            @Param("diagnosticTestId") Long diagnosticTestId
    );

    /**
     * Batch đếm tổng số câu hỏi theo từng đề và section (Listening / Reading) để tránh N+1 query.
     * Trả về [diagnosticTestId, partSections, partName, questionCount].
     */
    @Query("SELECT dtq.diagnosticTest.id, p.sections, p.name, COUNT(dtq.id) " +
           "FROM DiagnosticTestQuestion dtq " +
           "JOIN dtq.question q " +
           "LEFT JOIN q.part p " +
           "WHERE dtq.diagnosticTest.id IN :testIds " +
           "GROUP BY dtq.diagnosticTest.id, p.sections, p.name")
    List<Object[]> countQuestionsByTestIdsAndSection(@Param("testIds") Collection<Long> testIds);
}

