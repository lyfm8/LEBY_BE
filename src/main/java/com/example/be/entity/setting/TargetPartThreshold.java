package com.example.be.entity.setting;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.diagnostic.EPartDirective;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngưỡng điểm của từng Part trong một TargetProfile (AIM).
 * Được dùng để đánh giá kết quả Diagnostic và quyết định directive (PASS/CONFIRM/ABILITY_DIAGNOSTIC/FULL_PART).
 *
 * SƠ ĐỒ TRỤC ĐIỂM SỐ ĐÁNH GIÁ PART (Thang điểm 0% - 100%):
 *
 *   0%                     35%                     60%                 80%           100%
 *   |------ FULL_PART ------|-------- WEAK ---------|----- CONFIRM ------|---- PASS ---|
 *   [Mất gốc: học cả Part]  [Dưới chuẩn: soi kỹ]    [Cận chuẩn: soi nhẹ] [Đạt: miễn học]
 *                           ^                       ^                    ^
 *                   fullPartThreshold       confirmingScore           passScore
 *                   (Level 1 - Sàn đáy)   (Level 2 - Cận chuẩn)  (Level 3 - Đích đến)
 */
@Entity
@Table(name = "target_part_thresholds")
@Getter
@Setter
@NoArgsConstructor
public class TargetPartThreshold extends AbstractEntity {

    /**
     * Level 3: Điểm đạt chuẩn (PASS) theo AIM.
     * Nếu điểm thi >= passScore -> Miễn học toàn bộ Part này (0 module).
     */
    @Column(nullable = false)
    private Float passScore;

    /**
     * Level 2: Điểm cận chuẩn (CONFIRM).
     * Nếu confirmingScore <= điểm thi < passScore -> Xuống Tầng 2 lọc khắt khe (developingThreshold).
     */
    @Column(nullable = true)
    private Float confirmingScore;

    /**
     * Level 1: Điểm sàn mất gốc (FULL_PART).
     * Nếu điểm thi < fullPartThreshold -> Bốc 100% module của Part, không cần lọc Tầng 2.
     */
    @Column(nullable = true)
    private Float fullPartThreshold;

    /**
     * TargetPartThreshold * ---- 1 TargetProfile.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;

    /**
     * TargetPartThreshold * ---- 1 Part (ngưỡng cho Part nào trong 7 Part TOEIC).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    /**
     * Hàm nghiệp vụ tự động phân loại kết quả điểm số thành Directive 4 nhánh Tầng 1.
     * Giúp Dev không cần phải tự viết if-else phức tạp, tránh sai sót logic so sánh.
     *
     * @param actualScore Điểm số phần thi của học viên (ví dụ 75.5f)
     * @return EPartDirective kết quả phân loại
     */
    public EPartDirective determineDirective(float actualScore) {
        if (this.passScore != null && actualScore >= this.passScore) {
            return EPartDirective.PASS;
        }
        if (this.confirmingScore != null && actualScore >= this.confirmingScore) {
            return EPartDirective.CONFIRM;
        }
        if (this.fullPartThreshold != null && actualScore < this.fullPartThreshold) {
            return EPartDirective.FULL_PART;
        }
        return EPartDirective.ABILITY_DIAGNOSTIC;
    }
}
