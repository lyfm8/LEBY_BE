package com.example.be.features.content.entity;

import com.example.be.features.question.entity.PracticeQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Lesson dạng bài tập thực hành.
 * Extends Lesson (JOINED table strategy).
 * Rule 20: Phải dùng Java inheritance, không tạo Entity riêng độc lập.
 */
@Entity
@Table(name = "practice_lessons")
@Getter
@Setter
@NoArgsConstructor
public class PracticeLesson extends Lesson {

    /**
     * instructions: String - hướng dẫn thực hành
     */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /**
     * PracticeLesson 1 ---- * PracticeQuestion (composition).
     * PracticeQuestion phụ thuộc vòng đời vào PracticeLesson.
     * Rule 13: CascadeType.ALL + orphanRemoval.
     */
    @OneToMany(
            mappedBy = "practiceLesson",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PracticeQuestion> practiceQuestions = new ArrayList<>();
}
