package com.example.be.features.diagnostic.entity;

import com.example.be.features.content.entity.Part;
import com.example.be.features.diagnostic.enums.EPartDirective;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả của một Part sau khi làm bài DiagnosticAttempt Tầng 1.
 * Một lượt làm bài (DiagnosticAttempt) có thể sinh ra nhiều PartDiagnosticResult (cho nhiều Part khác nhau).
 */
@Entity
@Table(name = "part_diagnostic_results")
@Getter
@Setter
@NoArgsConstructor
public class PartDiagnosticResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EPartDirective directive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_attempt_id", nullable = false)
    private DiagnosticAttempt diagnosticAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;
}
