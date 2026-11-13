package com.example.be.repository.setting;

import com.example.be.entity.setting.AbilityEvaluationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbilityEvaluationRuleRepository extends JpaRepository<AbilityEvaluationRule, Long> {

    /**
     * Tìm quy tắc đánh giá năng lực theo Ability ID và TargetProfile ID.
     */
    Optional<AbilityEvaluationRule> findByAbilityIdAndTargetProfileId(Long abilityId, Long targetProfileId);

    /**
     * Lấy toàn bộ danh sách quy tắc đánh giá năng lực của một mục tiêu AIM.
     */
    List<AbilityEvaluationRule> findByTargetProfileId(Long targetProfileId);

    /**
     * Xóa toàn bộ quy tắc của một mục tiêu AIM (dùng khi reset/ghi đè).
     */
    void deleteByTargetProfileId(Long targetProfileId);
}
