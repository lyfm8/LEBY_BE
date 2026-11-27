package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
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
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.mockexam.service.UserMockExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý toàn bộ luồng Phòng thi thử TOEIC chuẩn ETS 200 câu (UC17)
 * và Báo cáo đầu ra Trước vs Sau (UC18) của học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   GET  /api/users/mock-exam/list               — Danh sách đề thi thử kèm tag & trạng thái mở khóa đề đầu ra
 *   POST /api/users/mock-exam/start              — Khởi tạo hoặc tiếp tục lượt thi thử 200 câu
 *   GET  /api/users/mock-exam/resume             — Khôi phục phiên thi thử đang diễn ra (nếu còn hạn)
 *   GET  /api/users/mock-exam/{attemptId}/exam   — Tải 200 câu hỏi, bản nháp & đồng hồ cho phòng thi
 *   PUT  /api/users/mock-exam/answers/draft      — Lưu nháp tức thì đáp án và cờ đánh dấu (BR12)
 *   POST /api/users/mock-exam/timer/sync         — Đồng bộ số giây làm bài còn lại (BR07 & Luồng 7a)
 *   POST /api/users/mock-exam/submit             — Nộp bài, chấm điểm chuẩn ETS 10-990
 *   GET  /api/users/mock-exam/report/latest      — Xem báo cáo đầu ra (OUTPUT) gần nhất
 *   GET  /api/users/mock-exam/{attemptId}/report — Xem báo cáo chi tiết / đối sánh Trước vs Sau (UC18)
 *   GET  /api/users/mock-exam/{attemptId}/review — Xem lại toàn bộ 200 câu kèm đáp án & lời giải (BR14)
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/mock-exam")
@RequiredArgsConstructor
public class UserMockExamController {

    private final UserMockExamService userMockExamService;

    /**
     * Lấy danh sách đề thi thử Full Mock Test (PRACTICE & OUTPUT) kèm lịch sử thi của học viên (UC17 Bước 2).
     */
    @GetMapping("/list")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamListResponse>> listAvailableMockExams(
            @AuthenticationPrincipal User currentUser
    ) {
        MockExamListResponse response = userMockExamService.listAvailableMockExams(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<MockExamListResponse>builder()
                .success(true)
                .message("Tải danh sách đề thi thử TOEIC thành công")
                .data(response)
                .build());
    }

    /**
     * Bắt đầu hoặc tiếp tục phiên làm bài thi thử TOEIC 200 câu (UC17 Bước 4-5).
     */
    @PostMapping("/start")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<StartDiagnosticResponse>> startMockExam(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody StartMockExamRequest request
    ) {
        StartDiagnosticResponse response = userMockExamService.startMockExam(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StartDiagnosticResponse>builder()
                        .success(true)
                        .message("Khởi tạo phiên thi thử TOEIC thành công")
                        .data(response)
                        .build());
    }

    /**
     * Khôi phục phiên thi thử đang diễn ra nếu còn thời gian hiệu lực (UC17 Luồng 7a).
     */
    @GetMapping("/resume")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamRoomResponse>> resumeMockExam(
            @AuthenticationPrincipal User currentUser
    ) {
        MockExamRoomResponse response = userMockExamService.getActiveSession(currentUser.getId());
        String message = response != null
                ? "Khôi phục phiên thi thử đang diễn ra"
                : "Không có phiên thi thử nào đang dang dở";
        return ResponseEntity.ok(ApiResponse.<MockExamRoomResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    /**
     * Tải danh sách 200 câu hỏi và đáp án nháp cho phòng thi thử.
     */
    @GetMapping("/{attemptId}/exam")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamRoomResponse>> getExamRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        MockExamRoomResponse response = userMockExamService.getExamRoom(currentUser.getId(), attemptId);
        return ResponseEntity.ok(ApiResponse.<MockExamRoomResponse>builder()
                .success(true)
                .message("Tải dữ liệu phòng thi thử thành công")
                .data(response)
                .build());
    }

    /**
     * Lưu nháp tức thì đáp án và cờ đánh dấu lên máy chủ để chống mất dữ liệu (BR12).
     */
    @PutMapping("/answers/draft")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Void>> saveDraftAnswers(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SaveMockExamDraftRequest request
    ) {
        userMockExamService.saveDraftAnswers(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đã lưu nháp bài thi thử")
                .data(null)
                .build());
    }

    /**
     * Đồng bộ số giây làm bài còn lại khi tạm dừng, định kỳ hoặc thoát trình duyệt (UC17 Luồng 7a).
     */
    @PostMapping("/timer/sync")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Long>> syncTimer(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SyncMockExamTimerRequest request
    ) {
        long remainingSeconds = userMockExamService.syncRemainingTime(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Đồng bộ thời gian làm bài thành công")
                .data(remainingSeconds)
                .build());
    }

    /**
     * Nộp bài thi thử 200 câu, chấm điểm quy đổi ETS 10-990 (UC17 Bước 10).
     */
    @PostMapping("/submit")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamSubmitResponse>> submitMockExam(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubmitMockExamRequest request
    ) {
        MockExamSubmitResponse response = userMockExamService.submitMockExam(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<MockExamSubmitResponse>builder()
                .success(true)
                .message("Đã hoàn thành và chấm điểm bài thi thử TOEIC")
                .data(response)
                .build());
    }

    /**
     * Lấy báo cáo nghiệm thu đầu ra (OUTPUT) gần nhất của học viên (UC18 Luồng 1a).
     */
    @GetMapping("/report/latest")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamReportResponse>> getLatestOutputReport(
            @AuthenticationPrincipal User currentUser
    ) {
        MockExamReportResponse response = userMockExamService.getLatestOutputReport(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<MockExamReportResponse>builder()
                .success(true)
                .message("Tải báo cáo nghiệm thu đầu ra gần nhất thành công")
                .data(response)
                .build());
    }

    /**
     * Lấy báo cáo kết quả và đối sánh Trước vs Sau theo attemptId (UC18).
     */
    @GetMapping("/{attemptId}/report")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamReportResponse>> getReport(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        MockExamReportResponse response = userMockExamService.getReport(currentUser.getId(), attemptId);
        return ResponseEntity.ok(ApiResponse.<MockExamReportResponse>builder()
                .success(true)
                .message("Tải báo cáo kết quả bài thi thử thành công")
                .data(response)
                .build());
    }

    /**
     * Xem lại chi tiết 200 câu hỏi, đáp án học viên chọn, đáp án đúng và giải thích (BR14).
     */
    @GetMapping("/{attemptId}/review")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MockExamReviewResponse>> getReviewData(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        MockExamReviewResponse response = userMockExamService.getReviewData(currentUser.getId(), attemptId);
        return ResponseEntity.ok(ApiResponse.<MockExamReviewResponse>builder()
                .success(true)
                .message("Tải dữ liệu xem lại bài thi thử thành công")
                .data(response)
                .build());
    }
}
