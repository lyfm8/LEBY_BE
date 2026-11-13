package com.example.be.service.exam.service;

import com.example.be.dto.request.exam.AutoSelectQuestionsRequest;
import com.example.be.dto.request.exam.CreateExamRequest;
import com.example.be.dto.request.exam.UpdateExamRequest;
import com.example.be.dto.response.exam.AutoSelectQuestionsResponse;
import com.example.be.dto.response.exam.ExamDetailResponse;
import com.example.be.dto.response.exam.ExamListItemResponse;
import com.example.be.dto.response.exam.ExamMatrixAvailabilityResponse;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service quản trị đề thi (Diagnostic Test và Mock Test).
 */
public interface AdminExamService {

    /**
     * Lấy danh sách đề thi theo bộ lọc và phân trang.
     *
     * @param keyword Từ khóa tìm kiếm theo tên đề
     * @param testType Loại đề thi (DIAGNOSTIC / FULL_MOCK_TEST)
     * @param examStatus Trạng thái (ACTIVE / ARCHIVE / DRAFT)
     * @param pageable Thông tin phân trang
     * @return Trang danh sách đề thi
     */
    Page<ExamListItemResponse> getExams(String keyword, ETestType testType, EExamStatus examStatus, Pageable pageable);

    /**
     * Lấy số lượng đề thi theo từng loại và trạng thái (phục vụ bộ đếm số lượng trên các Tabs).
     *
     * @return Map chứa { "all": count, "diagnostic": count, "mock": count }
     */
    Map<String, Long> getExamTabCounts();

    /**
     * Lấy thông tin chi tiết một bộ đề thi (kèm toàn bộ danh sách câu hỏi).
     *
     * @param id ID của đề thi
     * @return Chi tiết đề thi
     */
    ExamDetailResponse getExamById(Long id);

    /**
     * Tạo mới bộ đề thi.
     *
     * @param request Dữ liệu tạo đề
     * @param currentUsername Tên tài khoản thực hiện
     * @return Chi tiết đề vừa tạo
     */
    ExamDetailResponse createExam(CreateExamRequest request, String currentUsername);

    /**
     * Cập nhật thông tin và danh sách câu hỏi của đề thi.
     *
     * @param id ID của đề thi
     * @param request Dữ liệu cập nhật
     * @param currentUsername Tên tài khoản thực hiện
     * @return Chi tiết đề sau cập nhật
     */
    ExamDetailResponse updateExam(Long id, UpdateExamRequest request, String currentUsername);

    /**
     * Kích hoạt đề thi (ACTIVE).
     * QUY TẮC:
     * - Chỉ Admin mới có quyền thực hiện.
     * - Đối với Diagnostic Test: đề chẩn đoán đang Active trước đó sẽ tự động chuyển sang ARCHIVE.
     *
     * @param id ID của đề thi
     * @param currentUsername Tên tài khoản thực hiện
     */
    void activateExam(Long id, String currentUsername);

    /**
     * Lưu kho đề thi (ARCHIVE).
     * QUY TẮC:
     * - Chỉ Admin mới có quyền thực hiện.
     *
     * @param id ID của đề thi
     * @param currentUsername Tên tài khoản thực hiện
     */
    void archiveExam(Long id, String currentUsername);

    /**
     * Xóa đề thi (chỉ cho phép khi đề chưa từng có lượt làm bài nào).
     * QUY TẮC:
     * - Chỉ Admin mới có quyền thực hiện.
     *
     * @param id ID của đề thi
     */
    void deleteExam(Long id);

    /**
     * Kiểm tra số lượng câu hỏi khả dụng hiện có trong Question Bank theo từng Part và Ability.
     *
     * @return Thông tin khả dụng ma trận
     */
    ExamMatrixAvailabilityResponse getMatrixAvailability();

    /**
     * Tự động chọn câu hỏi ngẫu nhiên từ Question Bank khớp với ma trận chuẩn hoặc ma trận tùy chỉnh.
     *
     * @param request Yêu cầu cấu hình ma trận
     * @return Danh sách câu hỏi được chọn
     */
    AutoSelectQuestionsResponse autoSelectQuestions(AutoSelectQuestionsRequest request);
}

