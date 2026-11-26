package com.example.be.service.diagnostic.service;

import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SyncDiagnosticTimerRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;

/**
 * Service xử lý toàn bộ luồng phòng thi chẩn đoán (UC06), chấm điểm 2 tầng và báo cáo năng lực (UC07).
 */
public interface DiagnosticScoringService {

    /**
     * Bắt đầu một lượt làm bài mới cho học viên.
     * Kiểm tra điều kiện đề thi ACTIVE, kiểm tra BR02 theo mốc AIM hiện tại
     * và tạo bản ghi {@code DiagnosticAttempt} ở trạng thái {@code IN_PROGRESS}.
     *
     * @param userId  ID học viên đang đăng nhập
     * @param request Thông tin đề thi và mục tiêu AIM muốn thực hiện
     * @return Thông tin lượt thi vừa khởi tạo
     */
    StartDiagnosticResponse startDiagnostic(Long userId, StartDiagnosticRequest request);

    /**
     * Tải toàn bộ danh sách câu hỏi và cấu hình đồng hồ cho phiên làm bài đang diễn ra (UC06).
     *
     * @param userId    ID học viên đang đăng nhập (chống IDOR)
     * @param attemptId ID lượt làm bài cần nạp
     * @return {@link DiagnosticRoomResponse} chứa đề thi đã ẩn đáp án đúng
     */
    DiagnosticRoomResponse getExamForAttempt(Long userId, Long attemptId);

    /**
     * Kiểm tra và khôi phục phiên làm bài chẩn đoán đang diễn ra ({@code IN_PROGRESS}) của học viên (Luồng 5a).
     *
     * @param userId ID học viên đang đăng nhập
     * @return {@link DiagnosticRoomResponse} nếu có phiên dang dở, hoặc {@code null} nếu không có
     */
    DiagnosticRoomResponse getActiveSession(Long userId);

    /**
     * Đồng bộ số giây làm bài còn lại từ phòng thi lên máy chủ để ngưng đọng đồng hồ khi tạm dừng hoặc mất kết nối (Luồng 4b & 5a).
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Thông tin lượt thi và số giây còn lại từ client
     * @return Số giây còn lại hợp lệ đã được lưu trên máy chủ
     */
    long syncRemainingTime(Long userId, SyncDiagnosticTimerRequest request);

    /**
     * Nộp bài và thực hiện chấm điểm 2 tầng hoàn chỉnh.
     * <p>
     * Tầng 1: Tính điểm % từng Part → so sánh TargetPartThreshold → xác định directive
     * (PASS / CONFIRM / ABILITY_DIAGNOSTIC / FULL_PART).
     * Tầng 2: Với CONFIRM và ABILITY_DIAGNOSTIC, tính accuracyRate từng Ability →
     * so sánh AbilityEvaluationRule → phân loại WEAK / STABLE → lưu UserAbility.
     * Sau đó gọi AdaptivePathGeneratorService để sinh lộ trình cá nhân hóa.
     *
     * @param userId  ID học viên đang đăng nhập (chống IDOR)
     * @param request Dữ liệu nộp bài kèm đáp án và mục tiêu AIM
     * @return Kết quả chấm điểm và ID lộ trình vừa được sinh
     */
    DiagnosticResultResponse submitDiagnostic(Long userId, SubmitDiagnosticRequest request);

    /**
     * Lấy báo cáo kết quả chẩn đoán chi tiết và dữ liệu Radar 7 chiều theo ID lượt thi (UC07).
     *
     * @param userId    ID học viên đang đăng nhập (chống IDOR)
     * @param attemptId ID lượt thi chẩn đoán đã hoàn thành
     * @return {@link DiagnosticResultDetailResponse} phục vụ trang Báo cáo UC07
     */
    DiagnosticResultDetailResponse getResultDetail(Long userId, Long attemptId);

    /**
     * Lấy báo cáo kết quả chẩn đoán gần nhất đã hoàn thành của học viên (UC07 Luồng 1a & 1b).
     *
     * @param userId ID học viên đang đăng nhập
     * @return {@link DiagnosticResultDetailResponse} của lần thi đã nộp gần nhất
     */
    DiagnosticResultDetailResponse getLatestResultDetail(Long userId);
}
