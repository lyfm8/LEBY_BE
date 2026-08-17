package com.example.be.features.content.entity;
import com.example.be.features.user.entity.User;

import tools.jackson.databind.JsonNode;
import com.example.be.features.question.entity.ModuleTestQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Câu trả lời của User cho một ModuleTestQuestion trong một ModuleTestAttempt.
 */
@Entity
@Table(name = "module_test_answers")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isCorrect;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode userAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_attempt_id", nullable = false)
    private ModuleTestAttempt moduleTestAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_test_question_id", nullable = false)
    private ModuleTestQuestion moduleTestQuestion;
}
