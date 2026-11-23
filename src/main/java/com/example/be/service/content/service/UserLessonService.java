package com.example.be.service.content.service;

import com.example.be.dto.request.content.UpdateLessonProgressRequest;
import com.example.be.dto.response.content.LessonProgressResponse;
import com.example.be.dto.response.content.UserLessonDetailResponse;

/**
 * Service Interface cung cấp nghiệp vụ Phòng học bài giảng Video và ghi nhận tiến độ học (UC11).
 */
public interface UserLessonService {

    /**
     * Lấy đầy đủ dữ liệu phòng học bài giảng (nội dung video, tiến độ hiện tại để Resume,
     * danh sách bài học trong Module và bộ câu hỏi ôn tập củng cố) kèm kiểm tra khóa tuần tự BR10.
     *
     * @param userId   ID học viên đang đăng nhập (lấy từ SecurityContext, chống IDOR)
     * @param lessonId ID của bài học cần truy cập
     * @return {@link UserLessonDetailResponse} hoàn chỉnh cho giao diện phòng học
     */
    UserLessonDetailResponse getLessonDetailForUser(Long userId, Long lessonId);

    /**
     * Ghi nhận tiến độ xem bài giảng Video theo nhịp đồng bộ từ trình phát (chống tua theo BR09),
     * tự động đánh dấu hoàn thành khi đạt ngưỡng cấu hình và tính lại tiến độ mở khóa Module Test (BR03).
     *
     * @param userId   ID học viên đang đăng nhập
     * @param lessonId ID bài giảng Video đang học
     * @param request  Dữ liệu nhịp tiến độ (thời lượng xem tăng thêm, vị trí hiện tại, tốc độ phát)
     * @return {@link LessonProgressResponse} phản hồi trạng thái hoàn thành bài học và Module
     */
    LessonProgressResponse updateLessonProgress(Long userId, Long lessonId, UpdateLessonProgressRequest request);
}
