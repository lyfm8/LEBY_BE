package com.example.be.repository.question;

import com.example.be.entity.question.QuestionAbility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionAbilityRepository extends JpaRepository<QuestionAbility, Long> {

    long countByAbilityId(Long abilityId);

    /**
     * Lấy danh sách ID các Ability duy nhất được gắn vào tập hợp câu hỏi chỉ định.
     */
    @Query("SELECT DISTINCT qa.ability.id FROM QuestionAbility qa WHERE qa.question.id IN :questionIds")
    List<Long> findDistinctAbilityIdsByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);

    /**
     * Lấy toàn bộ QuestionAbility gắn với tập câu hỏi để hiển thị tên Ability.
     */
    @Query("SELECT qa FROM QuestionAbility qa JOIN FETCH qa.ability WHERE qa.question.id IN :questionIds")
    List<QuestionAbility> findWithAbilityByQuestionIdIn(@Param("questionIds") Collection<Long> questionIds);
}
