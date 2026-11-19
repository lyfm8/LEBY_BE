package com.example.be.repository.setting;

import com.example.be.entity.setting.AbilityEvaluationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbilityEvaluationRuleRepository extends JpaRepository<AbilityEvaluationRule, Long> {

    Optional<AbilityEvaluationRule> findByAbilityId(Long abilityId);

    @Query("SELECT r FROM AbilityEvaluationRule r JOIN FETCH r.ability a LEFT JOIN FETCH a.part p ORDER BY a.id ASC")
    List<AbilityEvaluationRule> findAllWithDetails();

    @Query("SELECT r FROM AbilityEvaluationRule r JOIN FETCH r.ability a LEFT JOIN FETCH a.part p WHERE r.id = :id")
    Optional<AbilityEvaluationRule> findByIdWithDetails(@Param("id") Long id);

    boolean existsByAbilityId(Long abilityId);

    boolean existsByAbilityIdAndIdNot(Long abilityId, Long id);
}
