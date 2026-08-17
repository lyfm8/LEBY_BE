package com.example.be.features.question.entity;
import com.example.be.features.question.entity.Question;

import com.example.be.features.content.entity.PracticeLesson;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Association entity: câu hỏi trong một PracticeLesson cụ thể.
 * PracticeQuestion KHÔNG phải subclass của Question.
 * Đây là entity trung gian giữa PracticeLesson và Question.
 * Rule 26: Phân biệt Inheritance vs Association.
 * Rule 28: orderNo là business ordering.
 */
@Entity
@Table(name = "practice_questions")
@Getter
@Setter
@NoArgsConstructor
public class PracticeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * orderNo: Int - thứ tự của câu hỏi trong PracticeLesson
     */
    @Column(nullable = false)
    private Integer orderNo;

    /**
     * points: Int - điểm của câu hỏi này trong bài luyện tập
     */
    private Integer points;

    /**
     * PracticeQuestion * ---- 1 PracticeLesson.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_lesson_id", nullable = false)
    private PracticeLesson practiceLesson;

    /**
     * PracticeQuestion * ---- 1 Question (references Question).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
