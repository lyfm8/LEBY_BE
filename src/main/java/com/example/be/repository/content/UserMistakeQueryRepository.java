package com.example.be.repository.content;

import com.example.be.entity.question.Question;
import com.example.be.enums.diagnostic.EAttemptStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repository truy vấn chỉ đọc (Projection) phục vụ gom dữ liệu Sổ tay câu hỏi làm sai (UC15)
 * từ lịch sử bài thi chẩn đoán, bài thi thử và bài thi vượt ải mô-đun mà không cần tạo thêm bảng mới
 * hay chỉnh sửa repository thuộc domain khảo thí.
 */
@org.springframework.stereotype.Repository
public interface UserMistakeQueryRepository extends Repository<Question, Long> {

    /**
     * Lấy danh sách câu trả lời sai từ các lượt thi chẩn đoán và thi thử 200 câu đã nộp của học viên.
     * Mỗi phần tử trả về gồm: [questionId, userAnswerJson, testType, testTitle, attemptId, answeredAt].
     *
     * @param userId            ID học viên đang đăng nhập
     * @param completedStatuses Các trạng thái lượt thi đã nộp (SUBMITTED, TIMED_OUT)
     * @return Danh sách bản ghi projection sắp xếp mới nhất trước
     */
    @Query("SELECT a.question.id, a.userAnswer, dt.testType, dt.title, da.id, " +
           "COALESCE(da.completedAt, a.updatedAt, a.createdAt) " +
           "FROM DiagnosticAnswer a " +
           "JOIN a.diagnosticAttempt da " +
           "JOIN da.diagnosticTest dt " +
           "WHERE da.user.id = :userId " +
           "AND da.status IN :completedStatuses " +
           "AND a.isCorrect = false " +
           "ORDER BY COALESCE(da.completedAt, a.updatedAt, a.createdAt) DESC")
    List<Object[]> findWrongDiagnosticAnswersByUserId(
            @Param("userId") Long userId,
            @Param("completedStatuses") Collection<EAttemptStatus> completedStatuses
    );

    /**
     * Lấy danh sách câu trả lời sai từ các lượt thi vượt ải mô-đun đã nộp của học viên.
     * Sử dụng trực tiếp {@code a.question.id} (schema V6) để bao phủ cả đề MANUAL lẫn RANDOM_BY_ABILITY.
     * Mỗi phần tử trả về gồm: [questionId, userAnswerJson, moduleTitle, attemptId, answeredAt].
     *
     * @param userId            ID học viên đang đăng nhập
     * @param completedStatuses Các trạng thái lượt thi đã nộp (SUBMITTED, TIMED_OUT)
     * @return Danh sách bản ghi projection sắp xếp mới nhất trước
     */
    @Query("SELECT a.question.id, a.userAnswer, m.title, mta.id, " +
           "COALESCE(mta.submittedAt, a.updatedAt, a.createdAt) " +
           "FROM ModuleTestAnswer a " +
           "JOIN a.moduleTestAttempt mta " +
           "JOIN mta.module m " +
           "WHERE mta.user.id = :userId " +
           "AND (mta.isSubmitted = true OR mta.status IN :completedStatuses) " +
           "AND a.isCorrect = false " +
           "ORDER BY COALESCE(mta.submittedAt, a.updatedAt, a.createdAt) DESC")
    List<Object[]> findWrongModuleTestAnswersByUserId(
            @Param("userId") Long userId,
            @Param("completedStatuses") Collection<EAttemptStatus> completedStatuses
    );

    /**
     * Nạp metadata nhẹ (Part và tên gợi nhớ) của tập câu hỏi sai để phục vụ lọc và thống kê bộ đếm
     * trước khi chỉ tải đầy đủ nội dung JSON & Media cho đúng trang cần hiển thị (chống N+1).
     * Mỗi phần tử trả về gồm: [questionId, partId, partName, questionName].
     *
     * @param questionIds Tập hợp ID câu hỏi sai duy nhất của học viên
     * @return Danh sách projection metadata câu hỏi
     */
    @Query("SELECT q.id, p.id, p.name, q.name " +
           "FROM Question q " +
           "LEFT JOIN q.part p " +
           "WHERE q.id IN :questionIds")
    List<Object[]> findQuestionMetaByIdIn(@Param("questionIds") Collection<Long> questionIds);
}
