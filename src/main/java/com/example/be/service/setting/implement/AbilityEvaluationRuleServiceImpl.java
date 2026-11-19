package com.example.be.service.setting.implement;

import com.example.be.dto.request.setting.CreateAbilityEvaluationRuleRequest;
import com.example.be.dto.request.setting.UpdateAbilityEvaluationRuleRequest;
import com.example.be.dto.response.setting.AbilityEvaluationRuleResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.setting.AbilityEvaluationRuleMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.service.setting.service.AbilityEvaluationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbilityEvaluationRuleServiceImpl implements AbilityEvaluationRuleService {

    private final AbilityEvaluationRuleRepository ruleRepo;
    private final AbilityRepository abilityRepo;
    private final AbilityEvaluationRuleMapper ruleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AbilityEvaluationRuleResponse> getAllRules() {
        log.info("Lấy toàn bộ danh sách quy tắc đánh giá Ability");
        List<AbilityEvaluationRule> rules = ruleRepo.findAllWithDetails();
        return rules.stream().map(ruleMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AbilityEvaluationRuleResponse getRuleById(Long id) {
        log.info("Lấy quy tắc đánh giá Ability theo ID: {}", id);
        AbilityEvaluationRule rule = ruleRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy tắc đánh giá với ID: " + id));
        return ruleMapper.toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public AbilityEvaluationRuleResponse getRuleByAbilityId(Long abilityId) {
        log.info("Lấy quy tắc đánh giá cho Ability ID: {}", abilityId);
        AbilityEvaluationRule rule = ruleRepo.findByAbilityId(abilityId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa thiết lập quy tắc cho Ability ID: " + abilityId));
        return ruleMapper.toResponse(rule);
    }

    @Override
    @Transactional
    public AbilityEvaluationRuleResponse createRule(CreateAbilityEvaluationRuleRequest request) {
        log.info("Tạo mới quy tắc đánh giá cho Ability ID: {}", request.getAbilityId());

        // Kiểm tra điều kiện nghiệp vụ: weakMax <= stableMin
        if (request.getWeakMax() > request.getStableMin()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Ngưỡng yếu (weakMax) không được vượt quá ngưỡng vững vàng (stableMin)");
        }

        if (ruleRepo.existsByAbilityId(request.getAbilityId())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Ability này đã có quy tắc đánh giá trong hệ thống");
        }

        Ability ability = abilityRepo.findById(request.getAbilityId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Ability với ID: " + request.getAbilityId()));

        AbilityEvaluationRule entity = ruleMapper.toEntity(request, ability);
        AbilityEvaluationRule saved = ruleRepo.save(entity);
        return ruleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AbilityEvaluationRuleResponse updateRule(Long id, UpdateAbilityEvaluationRuleRequest request) {
        log.info("Cập nhật quy tắc đánh giá ID: {}", id);

        if (request.getWeakMax() > request.getStableMin()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Ngưỡng yếu (weakMax) không được vượt quá ngưỡng vững vàng (stableMin)");
        }

        AbilityEvaluationRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy tắc đánh giá với ID: " + id));

        ruleMapper.updateEntity(rule, request);
        AbilityEvaluationRule saved = ruleRepo.save(rule);
        return ruleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        log.info("Xóa quy tắc đánh giá ID: {}", id);
        if (!ruleRepo.existsById(id)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quy tắc đánh giá với ID: " + id);
        }
        ruleRepo.deleteById(id);
    }
}
