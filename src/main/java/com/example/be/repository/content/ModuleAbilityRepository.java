package com.example.be.repository.content;

import com.example.be.entity.content.ModuleAbility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý quan hệ Nhiều-Nhiều giữa Module và Ability.
 */
@Repository
public interface ModuleAbilityRepository extends JpaRepository<ModuleAbility, Long> {

    List<ModuleAbility> findByModuleId(Long moduleId);

    void deleteByModuleId(Long moduleId);
}

