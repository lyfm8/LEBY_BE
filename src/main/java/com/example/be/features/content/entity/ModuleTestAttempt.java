package com.example.be.features.content.entity;
import com.example.be.features.content.entity.Module;

import com.example.be.features.diagnostic.enums.ETestResult;
import com.example.be.features.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho một lần Student thực hiện ModuleTest.
 * Rule 23: ModuleTestAttempt chứa đủ thông tin kết quả, không cần thêm ModuleTestResult.
 * Rule 40: Không duplicate Entity.
 * Rule 36: score + ModuleTest.passScore đủ để Service kiểm tra unlock condition.
 */
@Entity
@Table(name = "module_test_attempts")
@Getter
@Setter
@NoArgsConstructor
public class ModuleTestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * score: Int - điểm đạt được trong lần attempt này
     */
    private Integer score;

    @Enumerated(EnumType.STRING)
    private ETestResult result;

    /**
     * status: Boolean - attempt này đã submit chưa
     */
    @Column(nullable = false)
    private Boolean status = false;

    private LocalDate submittedAt;

    @OneToMany(
            mappedBy = "moduleTestAttempt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ModuleTestAnswer> answers = new ArrayList<>();

    /**
     * ModuleTestAttempt * ---- 1 Module.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * ModuleTestAttempt * ---- 1 User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
