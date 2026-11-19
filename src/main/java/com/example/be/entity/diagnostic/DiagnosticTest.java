package com.example.be.entity.diagnostic;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Đề thi đầu vào (Diagnostic Test) hoặc Đề thi thử tổng hợp (Mock Test).
 * Đóng vai trò là Khuôn mẫu (Master Data Blueprint).
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

    /**
     * Trạng thái đề thi: DRAFT, ACTIVE hoặc ARCHIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_status", nullable = false, length = 20)
    private EExamStatus examStatus = EExamStatus.DRAFT;

    /**
     * Phân loại đề thi: DIAGNOSTIC (chẩn đoán) hoặc FULL_MOCK_TEST (thi thử 200 câu).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 30)
    private ETestType testType = ETestType.DIAGNOSTIC;

    /**
     * Thời lượng làm bài (tính bằng phút), đọc động từ cấu hình đề.
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Thông tin người cập nhật gần nhất (vd: 'Admin' hoặc tên tài khoản).
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
