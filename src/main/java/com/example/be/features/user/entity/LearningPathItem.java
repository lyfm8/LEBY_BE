package com.example.be.features.user.entity;
import com.example.be.features.user.entity.User;

import com.example.be.features.content.entity.Module;
import com.example.be.features.content.enums.ELessonStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một item (Module) trong LearningPath của User.
 * Lưu thứ tự và trạng thái hoàn thành của từng module trong lộ trình.
 * Rule 28: orderNo là business ordering, không được thay bằng id.
 */
@Entity
@Table(name = "learning_path_items")
@Getter
@Setter
@NoArgsConstructor
public class LearningPathItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orderNo;

    /**
     * isCorrect: Boolean - từ diagram "isCorrect: Boolean"
     * Ý nghĩa nghiệp vụ: module này đã pass hay chưa trong lộ trình.
     */
    private Boolean isCorrect;

    /**
     * reason: String - lý do trạng thái (optional)
     */
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ELessonStatus status;

    /**
     * LearningPathItem * ---- 1 LearningPath.
     * Rule 10: Many-to-One, FK nằm ở LearningPathItem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    /**
     * LearningPathItem * ---- 1 Module (assign).
     * Một item trong lộ trình trỏ đến một Module cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
}
