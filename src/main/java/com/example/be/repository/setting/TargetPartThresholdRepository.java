package com.example.be.repository.setting;

import com.example.be.entity.setting.TargetPartThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetPartThresholdRepository extends JpaRepository<TargetPartThreshold, Long> {

    Optional<TargetPartThreshold> findByTargetProfileIdAndPartId(Long targetProfileId, Long partId);

    List<TargetPartThreshold> findByTargetProfileId(Long targetProfileId);
}
