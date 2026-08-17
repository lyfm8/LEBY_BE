package com.example.be.features.content.entity;
import com.example.be.features.content.entity.Module;

import com.example.be.features.ability.entity.Ability;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract base class cho Lesson (VideoLesson và PracticeLesson).
 * Rule 20: Dùng JPA Inheritance thay vì biến thành relationship.
 * Strategy: JOINED - VideoLesson và PracticeLesson có data riêng biệt.
 *
 * DECISION: Chọn JOINED strategy vì:
 * - VideoLesson có uri, duration riêng
 * - PracticeLesson có instructions, practiceQuestions riêng
 * - Tránh nullable columns dư thừa của SINGLE_TABLE
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public abstract class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Integer orderNo;

    @Column(columnDefinition = "TEXT")
    private String descriptions;

    /**
     * Lesson * ---- 1 Module.
     * FK nằm ở bảng lessons (base table).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    /**
     * Lesson "references" Ability - lesson này train ability nào.
     * Đây là aggregation, không cascade delete.
     * Rule 14: Aggregation không đồng nghĩa CascadeType.ALL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = true)
    private Ability ability;
}
