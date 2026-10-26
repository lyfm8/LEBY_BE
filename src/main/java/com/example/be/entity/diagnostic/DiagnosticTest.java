package com.example.be.entity.diagnostic;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.DiagnosticTestQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.be.enums.diagnostic.ETestType;

import java.util.ArrayList;
import java.util.List;

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
public class DiagnosticTest extends AbstractEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean status = true; // Đề này có đang được sử dụng hay không

    /**
     * Phân loại đề thi: DIAGNOSTIC (45 phút) hoặc FULL_MOCK_TEST (120 phút).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ETestType testType = ETestType.DIAGNOSTIC;

    /**
     * Thời lượng làm bài (tính bằng phút), đọc động từ cấu hình đề.
     */
    @Column(nullable = false)
    private Integer durationMinutes;

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
