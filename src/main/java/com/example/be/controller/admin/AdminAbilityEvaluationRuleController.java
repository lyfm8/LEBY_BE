package com.example.be.controller.admin;

import com.example.be.common.ApiResponse;
import com.example.be.dto.request.setting.CreateAbilityEvaluationRuleRequest;
import com.example.be.dto.request.setting.UpdateAbilityEvaluationRuleRequest;
import com.example.be.dto.response.setting.AbilityEvaluationRuleResponse;
import com.example.be.service.setting.service.AbilityEvaluationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ability-evaluation-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AdminAbilityEvaluationRuleController {

    private final AbilityEvaluationRuleService ruleService;

    @GetMapping
    public ApiResponse<List<AbilityEvaluationRuleResponse>> getAllRules() {
        return new ApiResponse<>(true, "Lấy danh sách quy tắc đánh giá Ability thành công", ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ApiResponse<AbilityEvaluationRuleResponse> getRuleById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Lấy chi tiết quy tắc đánh giá thành công", ruleService.getRuleById(id));
    }

    @GetMapping("/by-ability/{abilityId}")
    public ApiResponse<AbilityEvaluationRuleResponse> getRuleByAbilityId(@PathVariable Long abilityId) {
        return new ApiResponse<>(true, "Lấy quy tắc đánh giá theo Ability thành công", ruleService.getRuleByAbilityId(abilityId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AbilityEvaluationRuleResponse> createRule(
            @Valid @RequestBody CreateAbilityEvaluationRuleRequest request) {
        return new ApiResponse<>(true, "Tạo mới quy tắc đánh giá thành công", ruleService.createRule(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AbilityEvaluationRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAbilityEvaluationRuleRequest request) {
        return new ApiResponse<>(true, "Cập nhật quy tắc đánh giá thành công", ruleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return new ApiResponse<>(true, "Xóa quy tắc đánh giá thành công");
    }
}
