package com.example.be.service.content.service;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.ReorderLessonsRequest;
import com.example.be.dto.request.content.SaveLessonScriptsRequest;
import com.example.be.dto.request.content.SavePracticeQuestionsRequest;
import com.example.be.dto.request.content.UpdateLessonRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.dto.response.content.PracticeQuestionResponse;

import java.util.List;

/**
 * Service quản lý bài học (Lesson) và kịch bản phụ đề (Script) cho Quản trị viên và Giáo viên.
 */
public interface AdminLessonService {

    /**
     * Lấy toàn bộ danh sách bài học thuộc một Module cụ thể.
     *
     * @param moduleId ID của Module
     * @return Danh sách bài học sắp xếp theo orderNo
     */
    List<LessonSummaryResponse> getLessonsByModuleId(Long moduleId);

    /**
     * Lấy chi tiết bài học (bao gồm nội dung rich text và danh sách kịch bản phụ đề).
     *
     * @param lessonId ID của bài học
     * @return Chi tiết bài học
     */
    LessonDetailResponse getLessonById(Long lessonId);

    /**
     * Tạo bài học mới trong một Module.
     *
     * @param moduleId ID của Module chứa bài học
     * @param request Thông tin tạo bài học
     * @return Chi tiết bài học vừa tạo
     */
    LessonDetailResponse createLesson(Long moduleId, CreateLessonRequest request);

    /**
     * Cập nhật thông tin bài học.
     *
     * @param lessonId ID của bài học
     * @param request Thông tin cập nhật
     * @return Chi tiết bài học sau khi cập nhật
     */
    LessonDetailResponse updateLesson(Long lessonId, UpdateLessonRequest request);

    /**
     * Xóa một bài học.
     *
     * @param lessonId ID của bài học
     */
    void deleteLesson(Long lessonId);

    /**
     * Sắp xếp lại thứ tự bài học trong một Module sau khi kéo thả.
     *
     * @param moduleId ID của Module
     * @param request Danh sách ID bài học theo thứ tự mới
     */
    void reorderLessons(Long moduleId, ReorderLessonsRequest request);

    /**
     * Lưu hoặc cập nhật toàn bộ kịch bản phụ đề (Script) cho bài nghe Listening.
     *
     * @param lessonId ID của bài học
     * @param request Danh sách kịch bản mới
     * @return Chi tiết bài học kèm kịch bản phụ đề đã lưu
     */
    LessonDetailResponse saveLessonScripts(Long lessonId, SaveLessonScriptsRequest request);

    /**
     * Lấy danh sách câu hỏi luyện tập (Practice Questions) đang gắn với một bài học.
     *
     * @param lessonId ID của bài học
     * @return Danh sách câu hỏi luyện tập sắp xếp theo orderNo tăng dần
     */
    List<PracticeQuestionResponse> getPracticeQuestions(Long lessonId);

    /**
     * Đồng bộ toàn bộ danh sách câu hỏi luyện tập (Practice Questions) cho một bài học (Tab 3 - UC A05).
     * Tự động bóc tách chùm câu hỏi thành câu con làm bài được và cập nhật thứ tự/điểm số an toàn.
     *
     * @param lessonId ID của bài học
     * @param request  Danh sách câu hỏi luyện tập mới
     * @return Chi tiết bài học kèm danh sách câu hỏi luyện tập sau khi lưu
     */
    LessonDetailResponse savePracticeQuestions(Long lessonId, SavePracticeQuestionsRequest request);
}

