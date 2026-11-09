package com.example.be.enums.question;

/**
 * Phân loại hình thức tương tác của câu hỏi trong hệ thống LEBY.
 */
public enum EQuestionType {

    /**
     * Câu hỏi trắc nghiệm một đáp án đúng duy nhất (chuẩn toàn bộ đề thi TOEIC).
     */
    SINGLE_CHOICE,

    /**
     * Dạng điền khuyết (dự phòng phục vụ các bài tập bổ trợ ngữ pháp/từ vựng trong Lesson).
     */
    FILL_IN_BLANK
}
