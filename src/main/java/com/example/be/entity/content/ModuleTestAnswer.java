package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.ModuleTestQuestion;
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
 * Câu trả lời của User cho một ModuleTestQuestion trong một ModuleTestAttempt.
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
     * Đáp án học viên chọn dạng chuỗi JSON.
     */
    @Column(name = "user_answer", columnDefinition = "JSON")
    private String userAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_attempt_id", nullable = false)
    private ModuleTestAttempt moduleTestAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_question_id", nullable = false)
    private ModuleTestQuestion moduleTestQuestion;
}
