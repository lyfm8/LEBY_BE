package com.example.be.service.content.service;

import com.example.be.dto.request.content.SubmitMistakeRetryRequest;
import com.example.be.dto.response.content.MistakeNotebookResponse;
import com.example.be.dto.response.content.MistakeRetrySetResponse;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse;
import com.example.be.enums.content.EMistakeSourceType;

/**
 * Service Interface xử lý nghiệp vụ Sổ tay câu hỏi làm sai và phiên làm lại trực tiếp (UC15).
 */
public interface UserMistakeNotebookService {

    /**
     * Lấy danh sách câu hỏi làm sai của học viên (có lọc theo Part, Kỹ năng, Nguồn bài thi, Từ khóa và phân trang).
     *
     * @param userId     ID học viên đang đăng nhập (lấy từ SecurityContext để chống IDOR)
     * @param partId     Bộ lọc theo ID Phần thi (1..7, tùy chọn)
     * @param abilityId  Bộ lọc theo ID Kỹ năng (tùy chọn)
     * @param sourceType Bộ lọc theo nguồn bài thi phát sinh lỗi sai (tùy chọn)
     * @param keyword    Từ khóa tìm kiếm theo tên câu hỏi hoặc tên bài thi nguồn (tùy chọn)
     * @param page       Chỉ số trang bắt đầu từ 0
     * @param size       Số câu hỏi mỗi trang (1..50)
     * @return {@link MistakeNotebookResponse} chứa danh sách câu sai, phân trang và bộ đếm bộ lọc
     */
    MistakeNotebookResponse getMistakeNotebook(
            Long userId,
            Long partId,
            Long abilityId,
            EMistakeSourceType sourceType,
            String keyword,
            int page,
            int size
    );

    /**
     * Lấy bộ câu hỏi làm lại theo bộ lọc hiện tại cho phiên làm lại trực tiếp (UC15 Luồng 5a).
     * Bảo mật: Không trả đáp án đúng và lời giải trước khi học viên nộp bài.
     *
     * @param userId     ID học viên đang đăng nhập
     * @param partId     Bộ lọc theo ID Phần thi (tùy chọn)
     * @param abilityId  Bộ lọc theo ID Kỹ năng (tùy chọn)
     * @param sourceType Bộ lọc theo nguồn bài thi (tùy chọn)
     * @param keyword    Từ khóa tìm kiếm (tùy chọn)
     * @param limit      Số câu tối đa trong 1 phiên làm lại (1..50)
     * @return {@link MistakeRetrySetResponse} chứa bộ đề làm lại đã ẩn đáp án
     */
    MistakeRetrySetResponse getRetryQuestionSet(
            Long userId,
            Long partId,
            Long abilityId,
            EMistakeSourceType sourceType,
            String keyword,
            int limit
    );

    /**
     * Chấm điểm phía máy chủ cho toàn bộ câu trả lời học viên nộp trong phiên làm lại câu sai (UC15 Luồng 5a).
     *
     * @param userId  ID học viên đang đăng nhập (kiểm tra quyền sở hữu câu sai chống IDOR)
     * @param request Danh sách cặp questionId và phương án lựa chọn A/B/C/D
     * @return {@link MistakeRetrySubmitResponse} gồm điểm số tổng quan và lời giải chi tiết BR14 từng câu
     */
    MistakeRetrySubmitResponse submitRetryAnswers(Long userId, SubmitMistakeRetryRequest request);
}
