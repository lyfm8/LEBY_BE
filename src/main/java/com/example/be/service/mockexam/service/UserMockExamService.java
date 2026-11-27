package com.example.be.service.mockexam.service;

import com.example.be.dto.request.mockexam.SaveMockExamDraftRequest;
import com.example.be.dto.request.mockexam.StartMockExamRequest;
import com.example.be.dto.request.mockexam.SubmitMockExamRequest;
import com.example.be.dto.request.mockexam.SyncMockExamTimerRequest;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.dto.response.mockexam.MockExamListResponse;
import com.example.be.dto.response.mockexam.MockExamReportResponse;
import com.example.be.dto.response.mockexam.MockExamReviewResponse;
import com.example.be.dto.response.mockexam.MockExamRoomResponse;
import com.example.be.dto.response.mockexam.MockExamSubmitResponse;

/**
 * Service xử lý toàn bộ nghiệp vụ Phòng thi thử TOEIC chuẩn ETS 200 câu (UC17)
 * và Báo cáo nghiệm thu năng lực đầu ra Trước vs Sau (UC18).
 */
public interface UserMockExamService {

    /**
     * Lấy danh sách đề Full Mock Test đang ACTIVE kèm trạng thái mở khóa đề đầu ra và lịch sử thi (UC17 Bước 2).
     *
     * @param userId ID học viên đang đăng nhập
     * @return {@link MockExamListResponse}
     */
    MockExamListResponse listAvailableMockExams(Long userId);

    /**
     * Khởi tạo hoặc tiếp tục lượt thi thử TOEIC 200 câu cho đề thi được chọn (UC17 Bước 4-5).
     *
     * @param userId  ID học viên đang đăng nhập
     * @param request Yêu cầu chứa ID đề FULL_MOCK_TEST
     * @return {@link StartDiagnosticResponse}
     */
    StartDiagnosticResponse startMockExam(Long userId, StartMockExamRequest request);

    /**
     * Kiểm tra và khôi phục phiên thi thử đang diễn ra nếu còn thời gian (UC17 Luồng 7a).
     *
     * @param userId ID học viên đang đăng nhập
     * @return {@link MockExamRoomResponse} hoặc {@code null} nếu không có phiên đang mở
     */
    MockExamRoomResponse getActiveSession(Long userId);

    /**
     * Tải toàn bộ 200 câu hỏi, bản nháp đã lưu và đồng hồ đếm ngược cho một lượt thi thử.
     *
     * @param userId    ID học viên đang đăng nhập (chống IDOR)
     * @param attemptId ID lượt thi thử
     * @return {@link MockExamRoomResponse}
     */
    MockExamRoomResponse getExamRoom(Long userId, Long attemptId);

    /**
     * Lưu nháp tức thì các đáp án đã chọn và cờ đánh dấu lên máy chủ (BR12).
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Danh sách đáp án nháp cần lưu
     */
    void saveDraftAnswers(Long userId, SaveMockExamDraftRequest request);

    /**
     * Đồng bộ số giây còn lại của đồng hồ đếm ngược 120 phút (UC17 Luồng 7a).
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Số giây còn lại từ client
     * @return Số giây còn lại hợp lệ đã được lưu trên máy chủ
     */
    long syncRemainingTime(Long userId, SyncMockExamTimerRequest request);

    /**
     * Nộp bài thi thử 200 câu, chấm điểm quy đổi ETS 10-990 và lưu kết quả từng Part (UC17 Bước 10).
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Danh sách đáp án và thời gian làm bài
     * @return {@link MockExamSubmitResponse}
     */
    MockExamSubmitResponse submitMockExam(Long userId, SubmitMockExamRequest request);

    /**
     * Lấy báo cáo kết quả bài thi thử 200 câu hoặc Báo cáo nghiệm thu Trước vs Sau (UC18).
     *
     * @param userId    ID học viên đang đăng nhập (chống IDOR)
     * @param attemptId ID lượt thi thử đã hoàn thành
     * @return {@link MockExamReportResponse}
     */
    MockExamReportResponse getReport(Long userId, Long attemptId);

    /**
     * Lấy báo cáo của lượt thi đầu ra (OUTPUT) đã hoàn thành gần nhất (UC18 Luồng 1a).
     *
     * @param userId ID học viên đang đăng nhập
     * @return {@link MockExamReportResponse}
     */
    MockExamReportResponse getLatestOutputReport(Long userId);

    /**
     * Tải toàn bộ đề thi 200 câu kèm đáp án học viên chọn, đáp án đúng và giải thích để xem lại (BR14).
     *
     * @param userId    ID học viên đang đăng nhập (chống IDOR)
     * @param attemptId ID lượt thi thử đã nộp
     * @return {@link MockExamReviewResponse}
     */
    MockExamReviewResponse getReviewData(Long userId, Long attemptId);
}
