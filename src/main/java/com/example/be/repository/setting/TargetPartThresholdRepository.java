package com.example.be.repository.setting;

import com.example.be.entity.setting.TargetPartThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetPartThresholdRepository extends JpaRepository<TargetPartThreshold, Long> {

    /**
     * Tìm ngưỡng điểm đánh giá Part theo TargetProfile ID và Part ID.
     */
    Optional<TargetPartThreshold> findByTargetProfileIdAndPartId(Long targetProfileId, Long partId);

    /**
     * Lấy toàn bộ ngưỡng điểm 7 Part của một mục tiêu AIM.
     */
    List<TargetPartThreshold> findByTargetProfileId(Long targetProfileId);

    /**
     * Xóa toàn bộ ngưỡng điểm 7 Part của một mục tiêu AIM.
     */
    void deleteByTargetProfileId(Long targetProfileId);
}
