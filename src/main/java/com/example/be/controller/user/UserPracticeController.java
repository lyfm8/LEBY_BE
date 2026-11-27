package com.example.be.controller.user;

import com.example.be.common.ApiResponse;
import com.example.be.dto.response.exam.PracticeRoomResponse;
import com.example.be.entity.user.User;
import com.example.be.security.Roles;
import com.example.be.service.exam.service.UserPracticeService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API Phòng luyện tập bài thực hành tức thì (UC12) cho học viên.
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/users/practice/lessons/{lessonId} — Tải bộ câu hỏi thực hành kèm media/passage và lời giải tức thì
 * </pre>
 */
@Validated
@RestController
@RequestMapping("/api/users/practice")
@RequiredArgsConstructor
public class UserPracticeController {

    private final UserPracticeService userPracticeService;

    /**
     * Tải dữ liệu phòng luyện tập bài thực hành theo {@code lessonId}.
     *
     * @param currentUser Học viên đang đăng nhập (tiêm từ SecurityContext để chống IDOR)
     * @param lessonId    ID bài học thực hành (phải là số nguyên dương)
     * @return {@link ApiResponse} chứa {@link PracticeRoomResponse}
     */
    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize(Roles.ANY_AUTHENTICATED)
    public ResponseEntity<ApiResponse<PracticeRoomResponse>> getPracticeRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Mã bài học phải là số nguyên dương") Long lessonId
    ) {
        PracticeRoomResponse room = userPracticeService.getPracticeRoom(currentUser.getId(), lessonId);
        return ResponseEntity.ok(ApiResponse.<PracticeRoomResponse>builder()
                .success(true)
                .message("Tải dữ liệu phòng luyện tập thành công")
                .data(room)
                .build());
    }
}
