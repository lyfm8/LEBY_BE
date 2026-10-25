package com.example.be.entity.diagnostic;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.Question;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lưu câu trả lời chi tiết của học viên trong bài Diagnostic Test & Mock Test.
 */
@Entity
@Table(name = "diagnostic_answers")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticAnswer extends AbstractEntity {

    /**
     * Đáp án học viên chọn (ví dụ: {"selected": "A"}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode userAnswer;

    /**
     * Kết quả đúng hay sai.
     */
    @Column(nullable = false)
    private Boolean isCorrect;

    /**
     * Cờ đánh dấu câu hỏi này học viên có muốn xem lại không.
     */
    @Column(nullable = false)
    private Boolean isFlagged = false;

    /**
     * Lượt thi chứa câu trả lời này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_attempt_id", nullable = false)
    private DiagnosticAttempt diagnosticAttempt;

    /**
     * Câu hỏi được trả lời.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
