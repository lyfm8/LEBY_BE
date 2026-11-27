package com.example.be.service.exam.service;

import com.example.be.dto.request.exam.SaveModuleTestDraftRequest;
import com.example.be.dto.request.exam.SubmitModuleTestRequest;
import com.example.be.dto.request.exam.SyncModuleTestTimerRequest;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.ModuleTestRoomResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;

/**
 * Service xử lý toàn bộ nghiệp vụ Phòng thi vượt ải mô-đun (UC13)
 * và Xem kết quả / Xem lại bài thi vượt ải (UC14).
 */
public interface UserModuleTestService {

    /**
     * Khởi tạo lượt thi vượt ải mới hoặc khôi phục lượt thi đang diễn ra cho mô-đun (UC13).
     * Kiểm tra điều kiện mở khóa BR10, điều kiện tiên quyết BR03 và thực hiện xáo trộn đề theo BR05.
     */
    StartModuleTestResponse startModuleTest(Long userId, Long moduleId);

    /**
     * Kiểm tra và khôi phục phiên thi vượt ải đang diễn ra (UC13 Luồng 7a).
     * Trả về {@code null} nếu không có phiên hợp lệ hoặc tự động thu bài nếu phiên cũ đã hết giờ.
     */
    ModuleTestRoomResponse getActiveSession(Long userId, Long moduleId);

    /**
     * Tải danh sách câu hỏi phòng thi vượt ải và đáp án nháp cho một lượt thi đang mở.
     */
    ModuleTestRoomResponse getExamRoom(Long userId, Long moduleId, Long attemptId);

    /**
     * Đồng bộ số giây làm bài còn lại lên máy chủ khi tạm dừng, định kỳ hoặc thoát trang.
     */
    long syncRemainingTime(Long userId, Long moduleId, SyncModuleTestTimerRequest request);

    /**
     * Lưu nháp tức thì đáp án và cờ đánh dấu của học viên trong quá trình làm bài (UC13 Luồng 7).
     */
    void saveDraftAnswers(Long userId, Long moduleId, SaveModuleTestDraftRequest request);

    /**
     * Nộp bài thi vượt ải, chấm điểm, đối chiếu ngưỡng động theo AIM (BR04) và mở khóa mô-đun kế tiếp (BR10).
     */
    SubmitModuleTestResponse submitModuleTest(Long userId, Long moduleId, SubmitModuleTestRequest request);

    /**
     * Lấy báo cáo kết quả chi tiết và lịch sử các lần thi vượt ải của học viên trên mô-đun (UC14).
     */
    ModuleTestPerformanceReportResponse getPerformanceReport(Long userId, Long moduleId, Long attemptId);

    /**
     * Lấy dữ liệu xem lại chi tiết từng câu hỏi kèm đáp án và lời giải (UC14 Luồng 6a).
     */
    ModuleTestReviewResponse getTestReview(Long userId, Long moduleId, Long attemptId);
}
