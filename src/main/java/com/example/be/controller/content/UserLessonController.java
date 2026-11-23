package com.example.be.controller.content;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.content.UpdateLessonProgressRequest;
import com.example.be.dto.response.content.LessonProgressResponse;
import com.example.be.dto.response.content.UserLessonDetailResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.content.service.UserLessonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API Phòng học bài giảng Video và ghi nhận tiến độ học (UC11) cho học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/users/lessons/{lessonId}          — Tải chi tiết bài giảng, lịch sử xem & câu hỏi ôn tập
 *   PUT /api/users/lessons/{lessonId}/progress — Ghi nhận nhịp tiến độ xem Video (BR09) & cập nhật Module (BR03)
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/lessons")
@RequiredArgsConstructor
public class UserLessonController {

    private final UserLessonService userLessonService;

    /**
     * Lấy đầy đủ thông tin phòng học bài giảng Video, lịch sử tạm dừng (Resume) và câu hỏi ôn tập củng cố.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @param lessonId    ID bài học cần mở (bắt buộc là số nguyên dương)
     * @return {@link ApiResponse} chứa {@link UserLessonDetailResponse}
     */
    @GetMapping("/{lessonId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<UserLessonDetailResponse>> getLessonDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã bài học phải là số nguyên dương") Long lessonId
    ) {
        UserLessonDetailResponse detail = userLessonService.getLessonDetailForUser(currentUser.getId(), lessonId);
        return ResponseEntity.ok(ApiResponse.<UserLessonDetailResponse>builder()
                .success(true)
                .message("Tải nội dung bài giảng thành công")
                .data(detail)
                .build());
    }

    /**
     * Đồng bộ tiến độ xem bài giảng Video thực tế của học viên và tự động đánh giá hoàn thành (BR09 / BR03).
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext, chống IDOR)
     * @param lessonId    ID bài giảng Video đang học
     * @param request     Payload thời lượng xem tăng thêm và mốc tạm dừng hiện tại (đã validate bằng @Valid)
     * @return {@link ApiResponse} chứa {@link LessonProgressResponse}
     */
    @PutMapping("/{lessonId}/progress")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateLessonProgress(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã bài học phải là số nguyên dương") Long lessonId,
            @Valid @RequestBody UpdateLessonProgressRequest request
    ) {
        LessonProgressResponse response = userLessonService.updateLessonProgress(
                currentUser.getId(),
                lessonId,
                request
        );
        String message = Boolean.TRUE.equals(response.getJustCompleted())
                ? "Chúc mừng! Bạn đã hoàn thành bài giảng này"
                : "Đã lưu tiến độ học bài giảng";

        return ResponseEntity.ok(ApiResponse.<LessonProgressResponse>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }
}
