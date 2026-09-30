package com.example.be.entity.user;

import com.example.be.entity.content.Module;
import com.example.be.enums.content.ELessonStatus;
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
     * isCorrect: Boolean - module này đã pass hay chưa trong lộ trình.
     */
    private Boolean isCorrect;

    /**
     * progressPercent: Int - % bài học đã hoàn thành trong module này.
     * Cache để Dashboard hiển thị nhanh mà không cần JOIN tính lại.
     * Cập nhật mỗi lần user hoàn thành 1 lesson (POST /complete).
     */
    @Column(nullable = false)
    private Integer progressPercent = 0;

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
