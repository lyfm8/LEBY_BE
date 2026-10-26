package com.example.be.entity.content;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.enums.ability.ESection;
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
public class Part extends AbstractEntity {


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
