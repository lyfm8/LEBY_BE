package com.example.be.repository.setting;

import com.example.be.entity.setting.ModuleTargetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleTargetThresholdRepository extends JpaRepository<ModuleTargetThreshold, Long> {

    Optional<ModuleTargetThreshold> findByModuleIdAndTargetProfileId(Long moduleId, Long targetProfileId);

    @Query("SELECT m FROM ModuleTargetThreshold m JOIN FETCH m.module mod JOIN FETCH m.targetProfile tp")
    List<ModuleTargetThreshold> findAllWithDetails();

    @Query("SELECT m FROM ModuleTargetThreshold m JOIN FETCH m.module mod JOIN FETCH m.targetProfile tp WHERE mod.id IN :moduleIds")
    List<ModuleTargetThreshold> findByModuleIdIn(@Param("moduleIds") Collection<Long> moduleIds);

    void deleteByModuleIdAndTargetProfileId(Long moduleId, Long targetProfileId);
}
