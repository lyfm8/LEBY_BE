package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.ReorderLessonsRequest;
import com.example.be.dto.request.content.SaveLessonScriptsRequest;
import com.example.be.dto.request.content.SavePracticeQuestionsRequest;
import com.example.be.dto.request.content.UpdateLessonRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.dto.response.content.PracticeQuestionResponse;
import com.example.be.service.content.service.AdminLessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller phục vụ quản lý Bài học (Lesson) và Kịch bản phụ đề (Script) cho Quản trị viên và Giáo viên.
 * URL prefixes: /api/admin/lessons và /api/admin/modules/{moduleId}/lessons
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminLessonController {

    private final AdminLessonService adminLessonService;

    /**
     * Lấy danh sách bài học thuộc một Module.
     */
    @GetMapping("/modules/{moduleId}/lessons")
    public ApiResponse<List<LessonSummaryResponse>> getLessonsByModuleId(@PathVariable Long moduleId) {
        return new ApiResponse<>(true, "Lấy danh sách bài học thành công", adminLessonService.getLessonsByModuleId(moduleId));
    }

    /**
     * Tạo mới một bài học trong Module.
     */
    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonDetailResponse> createLesson(
            @PathVariable Long moduleId,
            @Valid @RequestBody CreateLessonRequest request
    ) {
        return new ApiResponse<>(true, "Tạo bài học thành công", adminLessonService.createLesson(moduleId, request));
    }

    /**
     * Sắp xếp lại thứ tự bài học trong một Module sau khi kéo thả.
     */
    @PatchMapping("/modules/{moduleId}/reorder-lessons")
    public ApiResponse<Void> reorderLessons(
            @PathVariable Long moduleId,
            @Valid @RequestBody ReorderLessonsRequest request
    ) {
        adminLessonService.reorderLessons(moduleId, request);
        return new ApiResponse<>(true, "Sắp xếp lại thứ tự bài học thành công");
    }

    /**
     * Lấy chi tiết bài học (bao gồm rich text descriptions và danh sách kịch bản phụ đề).
     */
    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<LessonDetailResponse> getLessonById(@PathVariable Long lessonId) {
        return new ApiResponse<>(true, "Lấy chi tiết bài học thành công", adminLessonService.getLessonById(lessonId));
    }

    /**
     * Cập nhật thông tin bài học.
     */
    @PutMapping("/lessons/{lessonId}")
    public ApiResponse<LessonDetailResponse> updateLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        return new ApiResponse<>(true, "Cập nhật bài học thành công", adminLessonService.updateLesson(lessonId, request));
    }

    /**
     * Xóa một bài học.
     */
    @DeleteMapping("/lessons/{lessonId}")
    public ApiResponse<Void> deleteLesson(@PathVariable Long lessonId) {
        adminLessonService.deleteLesson(lessonId);
        return new ApiResponse<>(true, "Xóa bài học thành công");
    }

    /**
     * Lưu hoặc cập nhật danh sách kịch bản phụ đề (Script) cho bài nghe Listening.
     */
    @PutMapping("/lessons/{lessonId}/scripts")
    public ApiResponse<LessonDetailResponse> saveLessonScripts(
            @PathVariable Long lessonId,
            @Valid @RequestBody SaveLessonScriptsRequest request
    ) {
        return new ApiResponse<>(true, "Lưu kịch bản phụ đề thành công", adminLessonService.saveLessonScripts(lessonId, request));
    }

    /**
     * Lấy danh sách câu hỏi luyện tập (Practice Questions) đã gắn vào một bài học (Tab 3 - UC A05).
     *
     * @param lessonId ID của bài học
     * @return Danh sách câu hỏi luyện tập sắp xếp theo thứ tự hiển thị
     */
    @GetMapping("/lessons/{lessonId}/practice-questions")
    public ApiResponse<List<PracticeQuestionResponse>> getPracticeQuestions(@PathVariable Long lessonId) {
        return new ApiResponse<>(
                true,
                "Lấy danh sách câu hỏi luyện tập thành công",
                adminLessonService.getPracticeQuestions(lessonId)
        );
    }

    /**
     * Đồng bộ danh sách câu hỏi luyện tập (Practice Questions) cho một bài học (Tab 3 - UC A05).
     *
     * @param lessonId ID của bài học
     * @param request  Danh sách câu hỏi cần gắn kèm thứ tự và điểm số
     * @return Chi tiết bài học sau khi lưu danh sách câu hỏi luyện tập
     */
    @PutMapping("/lessons/{lessonId}/practice-questions")
    public ApiResponse<LessonDetailResponse> savePracticeQuestions(
            @PathVariable Long lessonId,
            @Valid @RequestBody SavePracticeQuestionsRequest request
    ) {
        return new ApiResponse<>(
                true,
                "Lưu danh sách câu hỏi luyện tập thành công",
                adminLessonService.savePracticeQuestions(lessonId, request)
        );
    }
}

