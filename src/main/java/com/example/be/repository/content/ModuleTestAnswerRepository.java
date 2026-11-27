package com.example.be.repository.content;

import com.example.be.entity.content.ModuleTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Kho lưu trữ chi tiết câu trả lời bài thi vượt ải mô-đun (module_test_answers).
 * Dùng để kiểm tra ràng buộc toàn vẹn lịch sử thi trước khi gỡ câu hỏi khỏi đề vượt ải (UC A07)
 * và lưu nháp / chấm điểm / xem lại bài thi vượt ải (UC13 - UC14).
 */
@Repository
public interface ModuleTestAnswerRepository extends JpaRepository<ModuleTestAnswer, Long> {

    /**
     * Kiểm tra xem danh sách ID liên kết câu hỏi vượt ải đã có lượt trả lời nào từ học viên chưa.
     *
     * @param moduleTestQuestionIds Tập hợp ID của bảng module_test_questions
     * @return true nếu tồn tại ít nhất 1 câu trả lời tham chiếu tới các câu hỏi này
     */
    boolean existsByModuleTestQuestionIdIn(Collection<Long> moduleTestQuestionIds);

    /**
     * Trả về tập hợp các moduleTestQuestionId đã từng xuất hiện trong lịch sử làm bài của học viên.
     *
     * @param moduleTestQuestionIds Tập hợp ID của bảng module_test_questions cần kiểm tra
     * @return Tập hợp ID đang có lịch sử làm bài (không được phép xóa vật lý)
     */
    @Query("SELECT DISTINCT a.moduleTestQuestion.id FROM ModuleTestAnswer a WHERE a.moduleTestQuestion.id IN :ids")
    Set<Long> findUsedModuleTestQuestionIds(@Param("ids") Collection<Long> moduleTestQuestionIds);

    /**
     * Nạp toàn bộ câu trả lời kèm Question của một lượt thi vượt ải trong 1 câu truy vấn (tránh N+1).
     */
    @Query("SELECT a FROM ModuleTestAnswer a " +
           "JOIN FETCH a.question q " +
           "WHERE a.moduleTestAttempt.id = :attemptId")
    List<ModuleTestAnswer> findByModuleTestAttemptIdWithQuestion(@Param("attemptId") Long attemptId);
}
