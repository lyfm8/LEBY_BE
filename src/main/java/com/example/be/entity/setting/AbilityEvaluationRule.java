package com.example.be.entity.setting;

import com.example.be.entity.ability.Ability;
import com.example.be.entity.base.AbstractEntity;
import com.example.be.enums.diagnostic.EPartDirective;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rule đánh giá Năng lực (Ability) của User dựa trên kết quả bài thi chẩn đoán (Diagnostic).
 * Được Admin/Giáo viên cấu hình động trong DB, Service dùng để phân loại nhị phân Tầng 2.
 *
 * TRỤC ĐÁNH GIÁ NĂNG LỰC ABILITY:
 *   0%                      50%                     75%              100%
 *   |-------- WEAK ----------|----- DEVELOPING ------|----- STABLE -----|
 *   [Cần bốc module học]     [Đang phát triển]       [Vững vàng]
 *                            ^                       ^
 *                    developingThreshold       stableThreshold
 *                     (mẫu Seed Data 50%)    (mẫu Seed Data 75%)
 */
@Entity
@Table(name = "ability_evaluation_rules")
@Getter
@Setter
@NoArgsConstructor
public class AbilityEvaluationRule extends AbstractEntity {

    /**
     * Ngưỡng ổn định / vững vàng (STABLE) - dùng làm ngưỡng cắt an toàn cho nhánh WEAK (mẫu 75%).
     */
    @Column(nullable = false)
    private Float stableThreshold;

    /**
     * Ngưỡng đang phát triển (DEVELOPING) - dùng làm ngưỡng cắt khắt khe cho nhánh CONFIRM (mẫu 50%).
     */
    @Column(nullable = false)
    private Float developingThreshold;

    /**
     * Số bằng chứng câu hỏi tối thiểu để kết luận đánh giá (đủ độ tin cậy).
     */
    @Column(nullable = false)
    private Integer minEvidenceCount;

    /**
     * Quy tắc này áp dụng cho Năng lực (Ability) cụ thể nào.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_id", nullable = true)
    private Ability ability;

    /**
     * Hàm đánh giá nhị phân Tầng 2: Kiểm tra xem Năng lực này có bị xem là YẾU (cần bốc Module) hay không.
     *
     * @param accuracyRate Tỷ lệ trả lời đúng của học viên ở Ability này (0 - 100%)
     * @param directive Nhánh Tầng 1 của Part chứa Ability này (CONFIRM hoặc ABILITY_DIAGNOSTIC)
     * @return true nếu tỷ lệ đúng < ngưỡng cắt (cần học bổ trợ)
     */
    public boolean isWeak(float accuracyRate, EPartDirective directive) {
        float cutoff = (directive == EPartDirective.CONFIRM)
                ? this.developingThreshold
                : this.stableThreshold;
        return accuracyRate < cutoff;
    }


}
