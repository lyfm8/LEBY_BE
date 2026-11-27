package com.example.be.dto.response.diagnostic;

import com.example.be.dto.response.question.QuestionExplanationResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Dữ liệu chi tiết một câu hỏi trong chế độ Xem lại bài thi chẩn đoán (BR14).
 * Bao gồm đầy đủ ngữ cảnh câu hỏi, đáp án học viên đã chọn, đáp án đúng và lời giải chi tiết.
 */
@Getter
@Builder
public class DiagnosticReviewQuestionResponse {
    /** ID câu hỏi trong ngân hàng câu hỏi. */
    private Long questionId;
    /** Số thứ tự hiển thị của câu hỏi trong đề thi (1..N). */
    private Integer orderNo;
    /** ID câu hỏi cha nếu câu hỏi thuộc một chùm ngữ cảnh (Part 3, 4, 6, 7). */
    private Long parentId;
    /** ID phần thi Part. */
    private Long partId;
    /** Số thứ tự phần thi TOEIC (1..7). */
    private Integer partNo;
    /** Tên hiển thị của phần thi Part. */
    private String partName;
    /** Nhóm kỹ năng lớn: LISTENING hoặc READING. */
    private String section;
    /** Nội dung câu hỏi hoặc yêu cầu đề bài. */
    private String title;
    /** Lời dẫn/hướng dẫn ngắn của chùm câu hỏi (nếu có). */
    private String instruction;
    /** Nội dung đoạn văn đọc hiểu chung (chỉ áp dụng cho Part 6, Part 7). */
    private String passage;
    /** Đường dẫn tệp âm thanh bài nghe (nếu có). */
    private String audioUrl;
    /** Đường dẫn hình ảnh minh họa hoặc hình ảnh bài đọc (nếu có). */
    private String imageUrl;
    /** Cờ đánh dấu câu hỏi thuộc chùm ngữ cảnh (có parentId). */
    private Boolean isGroupQuestion;
    /** Danh sách các phương án lựa chọn A, B, C, D. */
    private List<DiagnosticOptionResponse> options;
    /** Ký hiệu đáp án học viên đã chọn (A, B, C, D) hoặc null nếu bỏ trống. */
    private String userAnswer;
    /** Ký hiệu đáp án đúng của câu hỏi (A, B, C, D). */
    private String correctAnswer;
    /** Kết quả học viên trả lời đúng hay sai. */
    private Boolean isCorrect;
    /** Trạng thái cờ đánh dấu xem lại từ lượt thi. */
    private Boolean isFlagged;
    /** Lời giải chi tiết (transcript, translation, clue, analysis, vocabulary). */
    private QuestionExplanationResponse explanation;
}
