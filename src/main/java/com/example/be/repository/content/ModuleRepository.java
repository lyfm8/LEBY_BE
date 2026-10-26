package com.example.be.repository.content;

import com.example.be.entity.content.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByPartIdOrderBySequenceAsc(Long partId);

    @Query("SELECT DISTINCT m FROM Module m JOIN m.moduleAbilities ma WHERE ma.ability.id = :abilityId ORDER BY m.sequence ASC")
    List<Module> findByAbilityId(@Param("abilityId") Long abilityId);
}
