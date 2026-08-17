package com.example.be.features.ability.entity;
import com.example.be.features.question.entity.Question;

import com.example.be.features.ability.enums.EElection;
import com.example.be.features.question.entity.QuestionAbility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho một Ability trong hệ thống TOEIC.
 * Ví dụ: Grammar, Vocabulary, Listening Comprehension, etc.
 */
@Entity
@Table(name = "abilities")
@Getter
@Setter
@NoArgsConstructor
public class Ability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String description;

    /**
     * sections: ESection - LISTENING hoặc READING.
     * Rule 43 (DECISION): Giá trị enum LISTENING/READING dựa trên context TOEIC.
     * Cần xác nhận lại nếu có thêm giá trị.
     */
    @Enumerated(EnumType.STRING)
    private ESection sections;

    /**
     * Ability 1 ---- * QuestionAbility (many-to-many trung gian với Question).
     * Rule 12: Dùng entity trung gian vì QuestionAbility có thêm attributes.
     */
    @OneToMany(mappedBy = "ability", fetch = FetchType.LAZY)
    private List<QuestionAbility> questionAbilities = new ArrayList<>();
}
