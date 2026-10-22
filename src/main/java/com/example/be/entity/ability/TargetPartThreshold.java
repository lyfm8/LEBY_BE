package com.example.be.entity.ability;

import com.example.be.entity.base.AbstractEntity;
import com.example.be.entity.content.Part;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngưỡng điểm của từng Part trong một TargetProfile.
 * Được dùng để đánh giá kết quả Diagnostic và quyết định directive (PASS/CONFIRM/...).
 * Rule 25: TargetPartThreshold là Domain Entity.
 */
@Entity
@Table(name = "target_part_thresholds")
@Getter
@Setter
@NoArgsConstructor
public class TargetPartThreshold extends AbstractEntity {

    /**
     * level1: Float - ngưỡng mức 1 (thấp nhất - dưới mức này là FULL_PART, cần học lại toàn bộ Part)
     */
    private Float level1;

    /**
     * level2: Float - ngưỡng mức 2 (tiếp theo - từ level1 đến level2 là WEAK)
     */
    private Float level2;

    /**
     * level3: Float - ngưỡng mức 3 (từ level2 đến level3 là CONFIRM / Đạt chuẩn)
     */
    private Float level3;

    /**
     * level4: Float - ngưỡng mức 4 (cao nhất - đạt hoặc vượt mục tiêu TARGET/PASS)
     */
    private Float level4;

    /**
     * TargetPartThreshold * ---- 1 TargetProfile.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_profile_id", nullable = false)
    private TargetProfile targetProfile;

    /**
     * TargetPartThreshold * ---- 1 Part (ngưỡng cho Part nào).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;
}
