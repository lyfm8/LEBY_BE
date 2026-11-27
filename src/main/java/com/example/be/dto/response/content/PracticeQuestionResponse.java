package com.example.be.dto.response.content;

import com.example.be.enums.ability.ESection;
import com.example.be.enums.question.EQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO phản hồi thông tin một câu hỏi luyện tập đã gắn vào bài học (Tab 3 - UC A05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionResponse {

    /**
     * ID của bản ghi liên kết PracticeQuestion trong bảng practice_questions.
     */
    private Long id;

    /**
     * ID câu hỏi gốc trong bảng questions.
     */
    private Long questionId;

    /**
     * Thứ tự hiển thị trong bài luyện tập.
     */
    private Integer orderNo;

    /**
     * Điểm số của câu hỏi trong bài luyện tập.
     */
    private Integer points;

    /**
     * Tên gợi nhớ của câu hỏi.
     */
    private String questionName;

    /**
     * Đoạn trích ngắn nội dung đề bài (bóc từ trường JSON questionData.text).
     */
    private String questionSummary;

    /**
     * Hình thức câu hỏi (ví dụ: SINGLE_CHOICE).
     */
    private EQuestionType questionType;

    /**
     * ID và tên Phần thi TOEIC (Part 1..7) của câu hỏi.
     */
    private Long partId;
    private String partName;

    /**
     * Phân khu kỹ năng (LISTENING / READING) của câu hỏi.
     */
    private ESection section;

    /**
     * Độ khó của câu hỏi (thang 1 - 5).
     */
    private Integer difficulty;

    /**
     * ID và tên ngữ cảnh cha nếu câu hỏi này thuộc một chùm câu hỏi (Part 3, 4, 6, 7).
     */
    private Long parentId;
    private String parentName;

    /**
     * Danh sách tên các Năng lực (Ability) mà câu hỏi này đánh giá.
     */
    private List<String> abilityNames;
}
