package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý luồng làm bài và nộp bài kiểm tra chẩn đoán của học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/users/diagnostic/start  — Bắt đầu bài chẩn đoán, tạo DiagnosticAttempt
 *   POST /api/users/diagnostic/submit — Nộp bài, chấm điểm 2 tầng, sinh lộ trình thích ứng
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
     * @param request     Thông tin đề thi muốn làm
     * @return Thông tin lượt thi vừa khởi tạo (attemptId, durationMinutes)
     */
    @PostMapping("/start")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<StartDiagnosticResponse>> startDiagnostic(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody StartDiagnosticRequest request
    ) {
        StartDiagnosticResponse response = diagnosticScoringService.startDiagnostic(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StartDiagnosticResponse>builder()
                        .success(true)
                        .message("Bắt đầu bài kiểm tra chẩn đoán thành công")
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
}
