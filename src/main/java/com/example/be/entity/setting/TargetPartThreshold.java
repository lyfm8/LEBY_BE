package com.example.be.entity.setting;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.TargetProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngưỡng điểm của từng Part trong một TargetProfile (AIM).
 * Lưu trữ trạng thái và quan hệ dữ liệu phục vụ đánh giá kết quả Diagnostic Test.
 *
 * SƠ ĐỒ TRỤC ĐIỂM SỐ ĐÁNH GIÁ PART (Thang điểm 0% - 100%):
 *   0%                confirmingScore              passScore           100%
 *   |─────── WEAK ──────────|─────── CONFIRM ──────────|──── PASS ────|
 *   [Học cả Part]           [Soi Ability]              [Miễn học]
 */
@Entity
@Table(
        name = "target_part_thresholds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_part_threshold_part_target",
                columnNames = {"part_id", "target_profile_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TargetPartThreshold extends AbstractEntity {

    /**
     * Mốc 2: Điểm đạt chuẩn (PASS) theo AIM.
     * Nếu actualScore >= passScore -> Miễn học toàn bộ Part này (0 module).
     */
    @Column(nullable = false)
    private Float passScore;

    /**
     * Mốc 1: Điểm cận chuẩn (CONFIRM).
     * Nếu confirmingScore <= actualScore < passScore -> Xuống Tầng 2 lọc theo Ability.
     * Nếu actualScore < confirmingScore -> WEAK (Bốc 100% module của Part).
     */
    @Column(nullable = false)
    private Float confirmingScore;

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
}
