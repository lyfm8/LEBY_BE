package com.example.be.features.content.entity;
import com.example.be.features.question.entity.Question;

import com.example.be.features.ability.enums.EElection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đại diện cho một Part trong bài thi TOEIC (Part 1 đến Part 7).
 * Part "contains" các Question (aggregation theo diagram).
 */
@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer partNo;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * sections: ESection - section TOEIC của Part này (LISTENING/READING)
     */
    @Enumerated(EnumType.STRING)
    private ESection sections;
}
