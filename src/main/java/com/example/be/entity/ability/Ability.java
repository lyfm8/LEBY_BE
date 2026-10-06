package com.example.be.entity.ability;

import com.example.be.enums.ability.ESection;
import com.example.be.entity.question.QuestionAbility;
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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ESection sections;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private com.example.be.entity.content.Part part;

    /**
     * Ability 1 ---- * QuestionAbility (many-to-many trung gian với Question).
     * Rule 12: Dùng entity trung gian vì QuestionAbility có thêm attributes.
     */
    @OneToMany(mappedBy = "ability", fetch = FetchType.LAZY)
    private List<QuestionAbility> questionAbilities = new ArrayList<>();
}
