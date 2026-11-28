package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.content.SubmitMistakeRetryRequest;
import com.example.be.dto.response.content.MistakeNotebookResponse;
import com.example.be.dto.response.content.MistakeRetrySetResponse;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse;
import com.example.be.entity.user.User;
import com.example.be.enums.content.EMistakeSourceType;
import com.example.be.security.Roles;
import com.example.be.service.content.service.UserMistakeNotebookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API Sổ tay câu hỏi làm sai và phiên làm lại trực tiếp tích hợp trên trang Hồ sơ (UC15).
 *
 * <p>Endpoints:
 * <pre>
 *   GET  /api/users/mistakes              — Lấy danh sách câu hỏi làm sai (lọc theo Part/Ability/Nguồn + phân trang)
 *   GET  /api/users/mistakes/retry-set    — Lấy bộ câu hỏi làm lại theo bộ lọc (ẩn đáp án & lời giải)
 *   POST /api/users/mistakes/retry-submit — Nộp bài làm lại và chấm điểm tức thì kèm lời giải BR14
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/mistakes")
@RequiredArgsConstructor
public class UserMistakeNotebookController {

    private final UserMistakeNotebookService userMistakeNotebookService;

    /**
     * Lấy danh sách câu hỏi làm sai của học viên đang đăng nhập kèm tùy chọn bộ lọc và phân trang.
     *
     * @param currentUser Học viên hiện tại tiêm từ SecurityContext (chống IDOR)
     * @param partId      Mã Phần thi cần lọc (tùy chọn)
     * @param abilityId   Mã Kỹ năng cần lọc (tùy chọn)
     * @param sourceType  Nguồn bài thi cần lọc (tùy chọn)
     * @param keyword     Từ khóa tìm kiếm (tối đa 100 ký tự, tùy chọn)
     * @param page        Trang hiện tại (bắt đầu từ 0)
     * @param size        Số câu mỗi trang (1..50, mặc định 10)
     * @return {@link ApiResponse} bọc {@link MistakeNotebookResponse}
     */
    @GetMapping
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MistakeNotebookResponse>> getMistakeNotebook(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @Positive(message = "Mã phần thi phải là số nguyên dương") Long partId,
            @RequestParam(required = false) @Positive(message = "Mã kỹ năng phải là số nguyên dương") Long abilityId,
            @RequestParam(required = false) EMistakeSourceType sourceType,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự") String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Chỉ số trang không được âm") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
            @Max(value = 50, message = "Kích thước trang tối đa là 50") int size
    ) {
        MistakeNotebookResponse response = userMistakeNotebookService.getMistakeNotebook(
                currentUser.getId(),
                partId,
                abilityId,
                sourceType,
                keyword,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.<MistakeNotebookResponse>builder()
                .success(true)
                .message("Tải sổ tay câu hỏi làm sai thành công")
                .data(response)
                .build());
    }

    /**
     * Lấy bộ câu hỏi làm lại theo bộ lọc hiện tại (ẩn hoàn toàn đáp án đúng và lời giải trước khi nộp bài).
     *
     * @param currentUser Học viên hiện tại tiêm từ SecurityContext
     * @param partId      Mã Phần thi cần lọc (tùy chọn)
     * @param abilityId   Mã Kỹ năng cần lọc (tùy chọn)
     * @param sourceType  Nguồn bài thi cần lọc (tùy chọn)
     * @param keyword     Từ khóa tìm kiếm (tùy chọn)
     * @param limit       Số câu tối đa cho phiên làm lại (1..50, mặc định 20)
     * @return {@link ApiResponse} bọc {@link MistakeRetrySetResponse}
     */
    @GetMapping("/retry-set")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MistakeRetrySetResponse>> getRetrySet(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @Positive(message = "Mã phần thi phải là số nguyên dương") Long partId,
            @RequestParam(required = false) @Positive(message = "Mã kỹ năng phải là số nguyên dương") Long abilityId,
            @RequestParam(required = false) EMistakeSourceType sourceType,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự") String keyword,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Số câu làm lại tối thiểu là 1")
            @Max(value = 50, message = "Số câu làm lại tối đa là 50") int limit
    ) {
        MistakeRetrySetResponse response = userMistakeNotebookService.getRetryQuestionSet(
                currentUser.getId(),
                partId,
                abilityId,
                sourceType,
                keyword,
                limit
        );
        return ResponseEntity.ok(ApiResponse.<MistakeRetrySetResponse>builder()
                .success(true)
                .message("Khởi tạo bộ câu hỏi làm lại thành công")
                .data(response)
                .build());
    }

    /**
     * Chấm điểm phía máy chủ cho nguyên bộ câu sai học viên vừa làm lại và trả lời giải chi tiết BR14.
     *
     * @param currentUser Học viên hiện tại tiêm từ SecurityContext
     * @param request     Danh sách câu trả lời nộp lên (đã validate qua @Valid)
     * @return {@link ApiResponse} bọc {@link MistakeRetrySubmitResponse}
     */
    @PostMapping("/retry-submit")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<MistakeRetrySubmitResponse>> submitRetry(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubmitMistakeRetryRequest request
    ) {
        MistakeRetrySubmitResponse response = userMistakeNotebookService.submitRetryAnswers(
                currentUser.getId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.<MistakeRetrySubmitResponse>builder()
                .success(true)
                .message("Chấm điểm phiên làm lại câu hỏi sai thành công")
                .data(response)
                .build());
    }
}
