package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Câu trả lời của User cho một câu hỏi trong một ModuleTestAttempt.
 */
@Entity
@Table(name = "module_test_answers")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTestAnswer extends AbstractEntity {

    @Column(nullable = false)
    private Boolean isCorrect;

    /**
     * Cờ đánh dấu câu hỏi này học viên muốn xem lại trước khi nộp bài.
     */
    @Column(nullable = false)
    private Boolean isFlagged = false;

    /**
     * Thứ tự hiển thị của câu hỏi trong lượt thi (sau khi áp dụng shuffle nếu có).
     */
    @Column(name = "order_no")
    private Integer orderNo;

    /**
     * Đáp án học viên chọn dạng chuỗi JSON.
     */
    @Column(name = "user_answer", columnDefinition = "JSON")
    private String userAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_attempt_id", nullable = false)
    private ModuleTestAttempt moduleTestAttempt;

    /**
     * Liên kết tùy chọn tới module_test_questions (null nếu đề sinh từ chế độ RANDOM_BY_ABILITY).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_question_id", nullable = true)
    private ModuleTestQuestion moduleTestQuestion;

    /**
     * Liên kết trực tiếp tới câu hỏi gốc trong ngân hàng câu hỏi (hỗ trợ cả MANUAL và RANDOM_BY_ABILITY).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
