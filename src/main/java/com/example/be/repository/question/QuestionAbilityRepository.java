package com.example.be.repository.question;

import com.example.be.entity.question.QuestionAbility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionAbilityRepository extends JpaRepository<QuestionAbility, Long> {
    long countByAbilityId(Long abilityId);
}
