package com.example.be.service.exam.service;

import com.example.be.dto.response.exam.PracticeRoomResponse;

/**
 * Service cung cấp dữ liệu Phòng luyện tập tức thì (UC12) cho học viên.
 */
public interface UserPracticeService {

    /**
     * Tải toàn bộ câu hỏi thực hành kèm ngữ cảnh câu hỏi cha, media, đáp án đúng và lời giải chi tiết
     * cho một bài học loại {@code PRACTICE}.
     *
     * @param userId   ID học viên đang đăng nhập (kiểm tra lộ trình và khóa tuần tự BR10)
     * @param lessonId ID bài học thực hành
     * @return {@link PracticeRoomResponse}
     */
    PracticeRoomResponse getPracticeRoom(Long userId, Long lessonId);
}
