package com.example.be.entity.ability;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lưu trạng thái năng lực (Ability) hiện tại của một User.
 * Được cập nhật sau khi User thực hiện Diagnostic Test.
 * Rule 25: UserAbility là Domain Entity, không phải Use Case operation.
 */
@Entity
@Table(name = "user_abilities")
@Getter
@Setter
@NoArgsConstructor
public class UserAbility extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EAbilityStatus status;

    /**
     * evidenceCount: Int - số lần đã được đánh giá, dùng để tính độ tin cậy.
     */
    @Column(nullable = false)
    private Integer evidenceCount = 0;

    /**
     * accuracyRate: Float - tỉ lệ đúng
     */
    private Float accuracyRate;

    /**
     * UserAbility * ---- 1 User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * UserAbility * ---- 1 Ability.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = false)
    private Ability ability;

    /**
     * UserAbility * ---- 1 Part (Ability này được đo trong context của Part nào).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;
}
