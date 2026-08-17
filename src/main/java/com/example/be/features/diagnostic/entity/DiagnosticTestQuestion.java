package com.example.be.features.diagnostic.entity;

import com.example.be.features.question.entity.Question;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bảng trung gian (Association Entity) nối giữa Đề thi gốc (DiagnosticTest) 
 * và Câu hỏi (Question) trong thư viện.
 * Giúp Admin xác định thứ tự và điểm số của từng câu hỏi trong bộ đề.
 */
@Entity
@Table(name = "diagnostic_test_questions")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orderNo;

    @Column(nullable = false)
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_test_id", nullable = false)
    private DiagnosticTest diagnosticTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
