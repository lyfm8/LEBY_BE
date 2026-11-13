package com.example.be.service.setting.service;

import com.example.be.dto.request.setting.CreateAbilityEvaluationRuleRequest;
import com.example.be.dto.request.setting.UpdateAbilityEvaluationRuleRequest;
import com.example.be.dto.response.setting.AbilityEvaluationRuleResponse;

import java.util.List;

public interface AbilityEvaluationRuleService {

    List<AbilityEvaluationRuleResponse> getAllRules();

    AbilityEvaluationRuleResponse getRuleById(Long id);

    AbilityEvaluationRuleResponse getRuleByAbilityId(Long abilityId);

    AbilityEvaluationRuleResponse createRule(CreateAbilityEvaluationRuleRequest request);

    AbilityEvaluationRuleResponse updateRule(Long id, UpdateAbilityEvaluationRuleRequest request);

    void deleteRule(Long id);
}
