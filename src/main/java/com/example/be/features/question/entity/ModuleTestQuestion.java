package com.example.be.features.question.entity;
import com.example.be.features.content.entity.Module;
import com.example.be.features.question.entity.Question;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Association entity: câu hỏi trong một ModuleTest cụ thể.
 * ModuleTestQuestion KHÔNG phải subclass của Question.
 * Rule 26: Phân biệt Inheritance vs Association.
 * Rule 28: orderNo là business ordering.
 */
@Entity
@Table(name = "module_test_questions")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * orderNo: Int - thứ tự câu hỏi trong bài test
     * Rule 28: Không xóa orderNo.
     */
    @Column(nullable = false)
    private Integer orderNo;

    /**
     * points: Int - điểm câu hỏi này trong ModuleTest
     */
    @Column(nullable = false)
    private Integer points;

    /**
     * ModuleTestQuestion * ---- 1 Module.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * ModuleTestQuestion * ---- 1 Question.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
