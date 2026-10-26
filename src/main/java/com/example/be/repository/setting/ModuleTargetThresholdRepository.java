package com.example.be.repository.setting;

import com.example.be.entity.setting.ModuleTargetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuleTargetThresholdRepository extends JpaRepository<ModuleTargetThreshold, Long> {

    Optional<ModuleTargetThreshold> findByModuleIdAndTargetProfileId(Long moduleId, Long targetProfileId);
}
