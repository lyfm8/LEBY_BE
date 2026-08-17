package com.example.be.features.content.entity;

import com.example.be.features.ability.entity.Ability;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity trung gian cho quan hệ Many-to-Many giữa Module và Ability.
 * "Module trains via Ability" trong Class Diagram.
 * Rule 12: Dùng entity trung gian thay vì @ManyToMany.
 * (Hiện tại không có thuộc tính riêng trên quan hệ này, nhưng dùng entity trung gian
 *  để dễ mở rộng sau nếu cần thêm thuộc tính như weight, priority...)
 */
@Entity
@Table(name = "module_abilities")
@Getter
@Setter
@NoArgsConstructor
public class ModuleAbility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = false)
    private Ability ability;
}
