package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.exam.SaveModuleTestDraftRequest;
import com.example.be.dto.request.exam.SubmitModuleTestRequest;
import com.example.be.dto.request.exam.SyncModuleTestTimerRequest;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.ModuleTestRoomResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.exam.service.UserModuleTestService;
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
 * Controller xử lý toàn bộ luồng Phòng thi vượt ải mô-đun (UC13)
 * và Xem báo cáo kết quả / Xem lại bài thi vượt ải (UC14) của học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/users/modules/{moduleId}/test/start               — Khởi tạo hoặc khôi phục attempt thi vượt ải
 *   GET  /api/users/modules/{moduleId}/test/resume              — Khôi phục phiên thi đang diễn ra (nếu còn hạn)
 *   GET  /api/users/modules/{moduleId}/test/exam/{attemptId}    — Tải đề thi & đáp án nháp cho attempt
 *   PUT  /api/users/modules/{moduleId}/test/answers/draft       — Lưu nháp tức thì đáp án và cờ đánh dấu
 *   POST /api/users/modules/{moduleId}/test/timer/sync          — Đồng bộ giây còn lại (pause/heartbeat/unload)
 *   POST /api/users/modules/{moduleId}/test/submit              — Nộp bài, chấm điểm và mở khóa mô-đun tiếp
 *   GET  /api/users/modules/{moduleId}/test/results/{attemptId} — Xem báo cáo kết quả và lịch sử thi
 *   GET  /api/users/modules/{moduleId}/test/review/{attemptId}  — Xem lại chi tiết từng câu hỏi kèm giải thích
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/modules/{moduleId}/test")
@RequiredArgsConstructor
public class UserModuleTestController {

    private final UserModuleTestService userModuleTestService;

    /**
     * Bắt đầu hoặc tiếp tục phiên thi vượt ải mô-đun (UC13).
     */
    @PostMapping("/start")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<StartModuleTestResponse>> startModuleTest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId
    ) {
        StartModuleTestResponse response = userModuleTestService.startModuleTest(currentUser.getId(), moduleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StartModuleTestResponse>builder()
                        .success(true)
                        .message("Khởi tạo phiên thi vượt ải thành công")
                        .data(response)
                        .build());
    }

    /**
     * Khôi phục phiên thi vượt ải đang diễn ra nếu còn thời gian hiệu lực (UC13 Luồng 7a).
     */
    @GetMapping("/resume")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ModuleTestRoomResponse>> resumeModuleTest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId
    ) {
        ModuleTestRoomResponse response = userModuleTestService.getActiveSession(currentUser.getId(), moduleId);
        String message = response != null
                ? "Khôi phục phiên thi vượt ải đang diễn ra thành công"
                : "Không có phiên thi vượt ải nào đang diễn ra";
        return ResponseEntity.ok(ApiResponse.<ModuleTestRoomResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    /**
     * Tải toàn bộ câu hỏi và đáp án nháp cho một phiên thi vượt ải đang mở.
     */
    @GetMapping("/exam/{attemptId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ModuleTestRoomResponse>> getExamRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        ModuleTestRoomResponse response = userModuleTestService.getExamRoom(
                currentUser.getId(),
                moduleId,
                attemptId
        );
        return ResponseEntity.ok(ApiResponse.<ModuleTestRoomResponse>builder()
                .success(true)
                .message("Tải dữ liệu phòng thi vượt ải thành công")
                .data(response)
                .build());
    }

    /**
     * Lưu nháp tức thì đáp án đã chọn và cờ đánh dấu lên máy chủ (UC13 Luồng 7).
     */
    @PutMapping("/answers/draft")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Void>> saveDraftAnswers(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @Valid @RequestBody SaveModuleTestDraftRequest request
    ) {
        userModuleTestService.saveDraftAnswers(currentUser.getId(), moduleId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đã lưu nháp bài thi vượt ải")
                .data(null)
                .build());
    }

    /**
     * Đồng bộ số giây làm bài còn lại khi tạm dừng, định kỳ hoặc thoát trình duyệt (UC13 Luồng 7a).
     */
    @PostMapping("/timer/sync")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<Long>> syncTimer(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @Valid @RequestBody SyncModuleTestTimerRequest request
    ) {
        long remainingSeconds = userModuleTestService.syncRemainingTime(currentUser.getId(), moduleId, request);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Đồng bộ thời gian làm bài thành công")
                .data(remainingSeconds)
                .build());
    }

    /**
     * Nộp bài thi vượt ải, chấm điểm theo ngưỡng AIM và mở khóa mô-đun tiếp theo nếu đạt chuẩn (UC13 -> UC14).
     */
    @PostMapping("/submit")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<SubmitModuleTestResponse>> submitModuleTest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @Valid @RequestBody SubmitModuleTestRequest request
    ) {
        SubmitModuleTestResponse response = userModuleTestService.submitModuleTest(
                currentUser.getId(),
                moduleId,
                request
        );
        String message = Boolean.TRUE.equals(response.getIsPassed())
                ? "Chúc mừng! Bạn đã vượt qua bài thi của mô-đun"
                : "Đã chấm điểm bài thi vượt ải. Hãy xem kết quả để củng cố thêm";
        return ResponseEntity.ok(ApiResponse.<SubmitModuleTestResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    /**
     * Lấy báo cáo kết quả chi tiết và lịch sử các lần thi của mô-đun (UC14).
     */
    @GetMapping("/results/{attemptId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ModuleTestPerformanceReportResponse>> getPerformanceReport(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        ModuleTestPerformanceReportResponse response = userModuleTestService.getPerformanceReport(
                currentUser.getId(),
                moduleId,
                attemptId
        );
        return ResponseEntity.ok(ApiResponse.<ModuleTestPerformanceReportResponse>builder()
                .success(true)
                .message("Tải báo cáo kết quả bài thi vượt ải thành công")
                .data(response)
                .build());
    }

    /**
     * Xem lại chi tiết từng câu hỏi, đáp án học viên chọn, đáp án đúng và lời giải (UC14 Luồng 6a).
     */
    @GetMapping("/review/{attemptId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<ModuleTestReviewResponse>> getTestReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã mô-đun phải là số nguyên dương") Long moduleId,
            @PathVariable @Positive(message = "Mã lượt thi phải là số nguyên dương") Long attemptId
    ) {
        ModuleTestReviewResponse response = userModuleTestService.getTestReview(
                currentUser.getId(),
                moduleId,
                attemptId
        );
        return ResponseEntity.ok(ApiResponse.<ModuleTestReviewResponse>builder()
                .success(true)
                .message("Tải dữ liệu xem lại bài thi vượt ải thành công")
                .data(response)
                .build());
    }
}
