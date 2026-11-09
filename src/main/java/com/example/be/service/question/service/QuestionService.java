package com.example.be.service.question.service;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.UpdateQuestionGroupRequest;
import com.example.be.dto.request.question.UpdateStandaloneQuestionRequest;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.dto.response.question.QuestionListItemResponse;
import com.example.be.enums.ability.ESection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Giao diện dịch vụ quản lý Ngân hàng câu hỏi (UC A04).
 */
public interface QuestionService {

    /**
     * Lấy danh sách câu hỏi gốc trong ngân hàng với bộ lọc đa tiêu chí và phân trang.
     *
     * @param keyword Từ khóa tìm kiếm theo tên hoặc nội dung
     * @param section Phần thi (LISTENING hoặc READING)
     * @param partId ID của Part
     * @param abilityId ID của Ability
     * @param difficulty Mức độ khó (1 đến 5)
     * @param structureType Lọc cấu trúc: "ALL", "STANDALONE" (câu đơn) hoặc "GROUP" (chùm câu hỏi)
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Trang danh sách QuestionListItemResponse
     */
    Page<QuestionListItemResponse> getQuestions(
            String keyword,
            ESection section,
            Long partId,
            Long abilityId,
            Integer difficulty,
            String structureType,
            Pageable pageable
    );

    /**
     * Lấy thông tin chi tiết của một câu hỏi (bao gồm câu đơn hoặc chùm câu hỏi kèm toàn bộ câu con).
     *
     * @param id ID của câu hỏi
     * @return QuestionDetailResponse
     */
    QuestionDetailResponse getQuestionById(Long id);

    /**
     * Tạo mới một câu hỏi đơn lập (Part 1, 2, 5).
     *
     * @param request Dữ liệu câu hỏi đơn
     * @return QuestionDetailResponse sau khi lưu
     */
    QuestionDetailResponse createStandaloneQuestion(CreateStandaloneQuestionRequest request);

    /**
     * Cập nhật thông tin câu hỏi đơn lập.
     *
     * @param id ID câu hỏi cần cập nhật
     * @param request Dữ liệu cập nhật
     * @return QuestionDetailResponse
     */
    QuestionDetailResponse updateStandaloneQuestion(Long id, UpdateStandaloneQuestionRequest request);

    /**
     * Tạo mới một chùm câu hỏi chung ngữ cảnh (Part 3, 4, 6, 7) kèm danh sách câu con.
     *
     * @param request Dữ liệu chùm câu hỏi
     * @return QuestionDetailResponse của chùm câu hỏi
     */
    QuestionDetailResponse createQuestionGroup(CreateQuestionGroupRequest request);

    /**
     * Cập nhật chùm câu hỏi và toàn bộ các câu hỏi con.
     *
     * @param id ID của chùm câu hỏi cha
     * @param request Dữ liệu cập nhật
     * @return QuestionDetailResponse
     */
    QuestionDetailResponse updateQuestionGroup(Long id, UpdateQuestionGroupRequest request);

    /**
     * Xóa một câu hỏi (câu đơn hoặc toàn bộ chùm câu hỏi kèm các câu con).
     * Sẽ bị chặn (HTTP 409) nếu câu hỏi đang được sử dụng trong bài thi hoạt động (Luồng ngoại lệ 2b).
     *
     * @param id ID của câu hỏi cần xóa
     */
    void deleteQuestion(Long id);
}

