package com.example.be.features.question.entity;

import com.example.be.features.ability.enums.ESection;
import com.example.be.features.content.entity.Part;
import com.example.be.features.question.enums.EQuestionType;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Question - base class cho tất cả câu hỏi trong hệ thống.
 * Strategy: JOINED - để normalize và tránh quá nhiều nullable columns.
 *
 * DECISION: Question là abstract domain entity với JOINED strategy.
 * PracticeQuestion, ModuleTestQuestion, DiagnosticAttemptQuestion là
 * association/join entities (KHÔNG phải subclass của Question).
 * Chúng reference Question nhưng thuộc về context riêng của chúng.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public abstract class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EQuestionType type;

    @Column(nullable = false, length = 200)
    private String name;

    /**
     * questionData: JSON - nội dung câu hỏi (text, hình ảnh, audio...)
     * Rule 17: Dùng JsonNode + @JdbcTypeCode(SqlTypes.JSON), không dùng String.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode questionData;

    /**
     * correctAnswer: JSON - đáp án đúng
     * Rule 17: Dùng JsonNode.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode correctAnswer;

    /**
     * difficulty: Int - độ khó (1-5)
     */
    private Integer difficulty;

    /**
     * sections: ESection - thuộc section nào (LISTENING/READING)
     */
    @Enumerated(EnumType.STRING)
    private ESection sections;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Question "references" Part - câu hỏi thuộc Part nào.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = true)
    private Part part;

    /**
     * Question 1 ---- * QuestionAbility (M-M với Ability qua entity trung gian).
     * Rule 12: Entity trung gian vì QuestionAbility có thuộc tính riêng.
     */
    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<QuestionAbility> questionAbilities = new ArrayList<>();
}
