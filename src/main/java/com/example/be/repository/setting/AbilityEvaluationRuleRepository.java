package com.example.be.repository.setting;

import com.example.be.entity.setting.AbilityEvaluationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AbilityEvaluationRuleRepository extends JpaRepository<AbilityEvaluationRule, Long> {

    Optional<AbilityEvaluationRule> findByAbilityId(Long abilityId);
}
