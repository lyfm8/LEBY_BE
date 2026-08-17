package com.example.be.features.question.entity;

import com.example.be.features.ability.entity.Ability;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity trung gian cho quan hệ Many-to-Many giữa Question và Ability.
 * "QuestionAbility measures Ability" trong diagram.
 * Rule 12: Dùng entity trung gian vì có thuộc tính riêng (orderNo, points).
 */
@Entity
@Table(name = "question_abilities")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAbility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * orderNo: Int - thứ tự trong context ability này (Rule 28)
     */
    private Integer orderNo;

    /**
     * points: Int - số điểm khi câu hỏi này được dùng để đánh giá ability này
     */
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = false)
    private Ability ability;
}
