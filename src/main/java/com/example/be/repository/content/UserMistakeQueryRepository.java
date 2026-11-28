package com.example.be.repository.content;

import com.example.be.entity.question.Question;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repository truy vấn chỉ đọc (Projection) phục vụ gom dữ liệu Sổ tay câu hỏi làm sai (UC15)
 * và tổng hợp Ma trận năng lực học viên (UC16) từ lịch sử bài thi chẩn đoán, bài thi thử và vượt ải
 * mà không cần tạo bảng mới hay chỉnh sửa repository thuộc domain khảo thí.
 */
@org.springframework.stereotype.Repository
public interface UserMistakeQueryRepository extends Repository<Question, Long> {

    /**
     * Lấy danh sách câu trả lời sai từ các lượt thi thử TOEIC (FULL_MOCK_TEST) đã nộp của học viên
     * kèm thông tin examTag và targetScore để kiểm soát khóa đáp án bài đầu ra (OUTPUT).
     * Tuyệt đối không lấy bài thi chẩn đoán đầu vào (DIAGNOSTIC) vào Sổ tay câu sai.
     * Mỗi phần tử gồm: [questionId, userAnswerJson, testTitle, attemptId, answeredAt, examTag, targetScore].
     *
     * @param userId            ID học viên đang đăng nhập
     * @param testType          Loại đề (ETestType.FULL_MOCK_TEST)
     * @param completedStatuses Các trạng thái lượt thi đã nộp (SUBMITTED, TIMED_OUT)
     * @return Danh sách bản ghi projection sắp xếp mới nhất trước
     */
    @Query("SELECT a.question.id, a.userAnswer, dt.title, da.id, " +
           "COALESCE(da.completedAt, a.updatedAt, a.createdAt), " +
           "dt.examTag, COALESCE(tp.targetTotalScore, uTp.targetTotalScore) " +
           "FROM DiagnosticAnswer a " +
           "JOIN a.diagnosticAttempt da " +
           "JOIN da.diagnosticTest dt " +
           "LEFT JOIN da.targetProfile tp " +
           "LEFT JOIN da.user u " +
           "LEFT JOIN u.targetProfile uTp " +
           "WHERE da.user.id = :userId " +
           "AND dt.testType = :testType " +
           "AND da.status IN :completedStatuses " +
           "AND a.isCorrect = false " +
           "ORDER BY COALESCE(da.completedAt, a.updatedAt, a.createdAt) DESC")
    List<Object[]> findWrongMockExamAnswersByUserId(
            @Param("userId") Long userId,
            @Param("testType") ETestType testType,
            @Param("completedStatuses") Collection<EAttemptStatus> completedStatuses
    );

    /**
     * Đếm tổng số câu và số câu đúng theo từng Part (kèm Section và tên Part) của các lượt thi đầu ra (OUTPUT)
     * trong 1 câu SQL duy nhất để tính chính xác điểm quy đổi TOEIC 990 phục vụ chốt chặn mở khóa review (0 N+1).
     * Mỗi phần tử gồm: [attemptId, partId, section, partName, totalQuestions, correctQuestions].
     *
     * @param attemptIds Tập hợp ID các lượt thi đầu ra (OUTPUT) cần kiểm tra
     * @return Danh sách thống kê theo cặp (attemptId, partId)
     */
    @Query("SELECT da.id, p.id, p.sections, p.name, COUNT(dtq.id), " +
           "SUM(CASE WHEN ans.isCorrect = true THEN 1 ELSE 0 END) " +
           "FROM DiagnosticAttempt da " +
           "JOIN DiagnosticTestQuestion dtq ON dtq.diagnosticTest.id = da.diagnosticTest.id " +
           "JOIN dtq.question q " +
           "JOIN q.part p " +
           "LEFT JOIN DiagnosticAnswer ans ON ans.diagnosticAttempt.id = da.id AND ans.question.id = q.id " +
           "WHERE da.id IN :attemptIds " +
           "GROUP BY da.id, p.id, p.sections, p.name")
    List<Object[]> countOutputAttemptPartStatsByAttemptIds(@Param("attemptIds") Collection<Long> attemptIds);

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

    /**
     * Tìm ID lượt thi chẩn đoán đầu vào (DIAGNOSTIC) đã hoàn thành gần nhất của học viên
     * phục vụ tổng hợp đủ Ma trận năng lực cả 7 Part (bao gồm các Part PASS / FULL_PART).
     *
     * @param userId            ID học viên
     * @param testType          Loại đề (ETestType.DIAGNOSTIC)
     * @param completedStatuses Trạng thái đã nộp (SUBMITTED, TIMED_OUT)
     * @param pageable          Giới hạn lấy đúng 1 bản ghi mới nhất
     * @return Danh sách chứa tối đa 1 attemptId mới nhất
     */
    @Query("SELECT da.id " +
           "FROM DiagnosticAttempt da " +
           "JOIN da.diagnosticTest dt " +
           "WHERE da.user.id = :userId " +
           "AND dt.testType = :testType " +
           "AND da.status IN :completedStatuses " +
           "ORDER BY COALESCE(da.completedAt, da.createdAt) DESC")
    List<Long> findLatestCompletedDiagnosticAttemptIds(
            @Param("userId") Long userId,
            @Param("testType") ETestType testType,
            @Param("completedStatuses") Collection<EAttemptStatus> completedStatuses,
            Pageable pageable
    );

    /**
     * Tổng hợp số câu và số câu đúng theo từng cặp (Ability, Part) trên toàn bộ 7 Part của 1 lượt chẩn đoán.
     * Tính cả câu bỏ trống vào tổng số câu (tính sai) để phản ánh đúng năng lực thực tế.
     * Mỗi phần tử gồm: [abilityId, abilityName, partId, totalQuestions, correctQuestions].
     *
     * @param attemptId ID lượt thi chẩn đoán hoàn thành gần nhất
     * @return Danh sách thống kê theo từng năng lực
     */
    @Query("SELECT ab.id, ab.name, q.part.id, COUNT(dtq.id), " +
           "SUM(CASE WHEN ans.isCorrect = true THEN 1 ELSE 0 END) " +
           "FROM DiagnosticAttempt da " +
           "JOIN DiagnosticTestQuestion dtq ON dtq.diagnosticTest.id = da.diagnosticTest.id " +
           "JOIN dtq.question q " +
           "JOIN q.questionAbilities qa " +
           "JOIN qa.ability ab " +
           "LEFT JOIN DiagnosticAnswer ans ON ans.diagnosticAttempt.id = da.id AND ans.question.id = q.id " +
           "WHERE da.id = :attemptId " +
           "AND q.part IS NOT NULL " +
           "GROUP BY ab.id, ab.name, q.part.id " +
           "ORDER BY q.part.id ASC, ab.name ASC")
    List<Object[]> aggregateDiagnosticAbilityStatsByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * Tổng hợp số câu và số câu đúng theo từng cặp (Ability, Part) tích lũy từ toàn bộ bài thi vượt ải mô-đun đã nộp.
     * Mỗi phần tử gồm: [abilityId, abilityName, partId, totalQuestions, correctQuestions].
     *
     * @param userId            ID học viên
     * @param completedStatuses Các trạng thái lượt thi đã nộp (SUBMITTED, TIMED_OUT)
     * @return Danh sách thống kê tích lũy từ bài thi mô-đun
     */
    @Query("SELECT ab.id, ab.name, q.part.id, COUNT(a.id), " +
           "SUM(CASE WHEN a.isCorrect = true THEN 1 ELSE 0 END) " +
           "FROM ModuleTestAnswer a " +
           "JOIN a.moduleTestAttempt mta " +
           "JOIN a.question q " +
           "JOIN q.questionAbilities qa " +
           "JOIN qa.ability ab " +
           "WHERE mta.user.id = :userId " +
           "AND (mta.isSubmitted = true OR mta.status IN :completedStatuses) " +
           "AND q.part IS NOT NULL " +
           "GROUP BY ab.id, ab.name, q.part.id " +
           "ORDER BY q.part.id ASC, ab.name ASC")
    List<Object[]> aggregateModuleTestAbilityStatsByUserId(
            @Param("userId") Long userId,
            @Param("completedStatuses") Collection<EAttemptStatus> completedStatuses
    );
}
