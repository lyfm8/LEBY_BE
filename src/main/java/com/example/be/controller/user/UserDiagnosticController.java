package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.diagnostic.service.DiagnosticScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý toàn bộ luồng Phòng thi chẩn đoán (UC06) và Xem báo cáo năng lực (UC07) của học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/users/diagnostic/start              — Bắt đầu bài chẩn đoán, tạo DiagnosticAttempt
 *   GET  /api/users/diagnostic/resume             — Khôi phục phiên làm bài đang diễn ra (nếu có)
 *   GET  /api/users/diagnostic/exam/{attemptId}   — Tải danh sách câu hỏi phòng thi cho lượt thi
 *   POST /api/users/diagnostic/submit             — Nộp bài, chấm điểm 2 tầng, sinh lộ trình thích ứng
 *   GET  /api/users/diagnostic/results/latest     — Xem báo cáo kết quả chẩn đoán gần nhất (UC07)
 *   GET  /api/users/diagnostic/results/{attemptId}— Xem chi tiết báo cáo kết quả theo attemptId (UC07)
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/diagnostic")
@RequiredArgsConstructor
public class UserDiagnosticController {

    private final DiagnosticScoringService diagnosticScoringService;

    /**
     * Bắt đầu bài kiểm tra chẩn đoán.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ Security Context)
     * @param request     Thông tin đề thi và mục tiêu AIM (tùy chọn)
     * @return Thông tin lượt thi vừa khởi tạo (attemptId, diagnosticTestId, targetProfileId, durationMinutes)
     */
    @PostMapping("/start")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<StartDiagnosticResponse>> startDiagnostic(
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) StartDiagnosticRequest request
    ) {
        StartDiagnosticRequest effectiveRequest = request != null ? request : new StartDiagnosticRequest();
        StartDiagnosticResponse response = diagnosticScoringService.startDiagnostic(currentUser.getId(), effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StartDiagnosticResponse>builder()
                        .success(true)
                        .message("Bắt đầu bài kiểm tra chẩn đoán thành công")
                        .data(response)
                        .build());
    }

    /**
     * Kiểm tra và khôi phục phiên làm bài chẩn đoán đang diễn ra (Luồng thay thế 5a - UC06).
     *
     * @param currentUser Học viên đang đăng nhập
     * @return Dữ liệu phòng thi nếu đang có phiên {@code IN_PROGRESS}, hoặc {@code null} nếu không có
     */
    @GetMapping("/resume")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<DiagnosticRoomResponse>> resumeDiagnostic(
            @AuthenticationPrincipal User currentUser
    ) {
        DiagnosticRoomResponse response = diagnosticScoringService.getActiveSession(currentUser.getId());
        String message = response != null
                ? "Khôi phục phiên làm bài chẩn đoán đang diễn ra thành công"
                : "Không có phiên làm bài chẩn đoán đang diễn ra";
        return ResponseEntity.ok(ApiResponse.<DiagnosticRoomResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    /**
     * Tải toàn bộ danh sách câu hỏi cho lượt thi chẩn đoán đã khởi tạo (UC06).
     *
     * @param currentUser Học viên đang đăng nhập (chống IDOR)
     * @param attemptId   ID lượt làm bài cần tải câu hỏi
     * @return Danh sách câu hỏi (ẩn đáp án đúng) kèm thời gian còn lại
     */
    @GetMapping("/exam/{attemptId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<DiagnosticRoomResponse>> getExamForAttempt(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long attemptId
    ) {
        DiagnosticRoomResponse response = diagnosticScoringService.getExamForAttempt(currentUser.getId(), attemptId);
        return ResponseEntity.ok(ApiResponse.<DiagnosticRoomResponse>builder()
                .success(true)
                .message("Tải dữ liệu đề thi chẩn đoán thành công")
                .data(response)
                .build());
    }

    /**
     * Nộp bài và nhận kết quả chấm điểm cùng lộ trình học tập cá nhân hóa.
     *
     * @param currentUser Học viên đang đăng nhập (chống IDOR — service kiểm tra userId khớp attempt)
     * @param request     Danh sách đáp án và mục tiêu AIM đã chọn
     * @return Điểm từng Part, directive, và ID lộ trình vừa được sinh ra
     */
    @PostMapping("/submit")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<DiagnosticResultResponse>> submitDiagnostic(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubmitDiagnosticRequest request
    ) {
        DiagnosticResultResponse response = diagnosticScoringService.submitDiagnostic(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<DiagnosticResultResponse>builder()
                .success(true)
                .message("Chấm điểm thành công. Lộ trình học tập cá nhân hóa đã được tạo")
                .data(response)
                .build());
    }

    /**
     * Lấy báo cáo kết quả chẩn đoán và Biểu đồ Radar gần nhất của học viên (UC07 Luồng 1a & 1b).
     *
     * @param currentUser Học viên đang đăng nhập
     * @return Báo cáo kết quả chi tiết của lượt thi đã nộp gần nhất
     */
    @GetMapping("/results/latest")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<DiagnosticResultDetailResponse>> getLatestResultDetail(
            @AuthenticationPrincipal User currentUser
    ) {
        DiagnosticResultDetailResponse response = diagnosticScoringService.getLatestResultDetail(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<DiagnosticResultDetailResponse>builder()
                .success(true)
                .message("Tải báo cáo chẩn đoán gần nhất thành công")
                .data(response)
                .build());
    }

    /**
     * Lấy báo cáo kết quả chẩn đoán và Biểu đồ Radar chi tiết theo attemptId (UC07).
     *
     * @param currentUser Học viên đang đăng nhập (chống IDOR)
     * @param attemptId   ID lượt thi chẩn đoán đã nộp
     * @return Báo cáo kết quả chi tiết 7 Part, Radar 7 chiều và khuyến nghị lộ trình
     */
    @GetMapping("/results/{attemptId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<DiagnosticResultDetailResponse>> getResultDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long attemptId
    ) {
        DiagnosticResultDetailResponse response = diagnosticScoringService.getResultDetail(currentUser.getId(), attemptId);
        return ResponseEntity.ok(ApiResponse.<DiagnosticResultDetailResponse>builder()
                .success(true)
                .message("Tải báo cáo kết quả chẩn đoán thành công")
                .data(response)
                .build());
    }
}
