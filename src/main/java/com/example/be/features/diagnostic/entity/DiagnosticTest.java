package com.example.be.features.diagnostic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import com.example.be.features.diagnostic.enums.ETestType;

/**
 * Đề thi đầu vào (Diagnostic Test) cố định do Admin soạn.
 * Đóng vai trò là Khuôn mẫu (Master Data Blueprint).
 * Admin có thể soạn nhiều đề (ví dụ: Đề test đầu vào số 1, Đề số 2...).
 */
@Entity
@Table(name = "diagnostic_tests")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean status = true; // Đề này có đang được sử dụng hay không

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ETestType testType;

    /**
     * Danh sách câu hỏi nằm trong Đề thi này.
     * Dùng CascadeType.ALL + orphanRemoval vì câu hỏi trong đề phụ thuộc hoàn toàn vào đề.
     */
    @OneToMany(
            mappedBy = "diagnosticTest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DiagnosticTestQuestion> testQuestions = new ArrayList<>();
}
