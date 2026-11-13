package com.example.be.repository.setting;

import com.example.be.entity.setting.TargetPartThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetPartThresholdRepository extends JpaRepository<TargetPartThreshold, Long> {

    Optional<TargetPartThreshold> findByTargetProfileIdAndPartId(Long targetProfileId, Long partId);

    List<TargetPartThreshold> findByTargetProfileId(Long targetProfileId);

    @Query("SELECT t FROM TargetPartThreshold t JOIN FETCH t.part p JOIN FETCH t.targetProfile tp ORDER BY tp.targetTotalScore ASC, p.id ASC")
    List<TargetPartThreshold> findAllWithDetails();

    @Query("SELECT t FROM TargetPartThreshold t JOIN FETCH t.part p JOIN FETCH t.targetProfile tp WHERE tp.id = :targetProfileId ORDER BY p.id ASC")
    List<TargetPartThreshold> findByTargetProfileIdWithDetails(@Param("targetProfileId") Long targetProfileId);

    boolean existsByTargetProfileIdAndPartId(Long targetProfileId, Long partId);

    boolean existsByTargetProfileIdAndPartIdAndIdNot(Long targetProfileId, Long partId, Long id);
}
