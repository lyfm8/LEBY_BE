package com.example.be.service.setting.implement;

import com.example.be.dto.request.setting.CopyThresholdsFromAimRequest;
import com.example.be.dto.request.setting.SaveAbilityRulesBatchRequest;
import com.example.be.dto.request.setting.SavePartThresholdsBatchRequest;
import com.example.be.dto.response.setting.AimAbilityRuleMatrixResponse;
import com.example.be.dto.response.setting.AimPartThresholdMatrixResponse;
import com.example.be.dto.response.setting.ThresholdValidationResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.setting.service.AdminThresholdConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản trị cấu hình Ngưỡng & Quy tắc đánh giá thích ứng (A08 & A10).
 * Tuân thủ Clean Code & kiến trúc phân tầng dự án LEBY.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminThresholdConfigServiceImpl implements AdminThresholdConfigService {

    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final TargetProfileRepository targetProfileRepo;
    private final PartRepository partRepo;
    private final AbilityRepository abilityRepo;

    // =========================================================================
    // A08: CẤU HÌNH NGƯỠNG ĐÁNH GIÁ PART THEO AIM
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AimPartThresholdMatrixResponse getPartThresholdMatrix(Long targetProfileId) {
        // Bước 1: Kiểm tra TargetProfile có tồn tại không
        TargetProfile targetProfile = findTargetProfileOrThrow(targetProfileId);

        // Bước 2: Nạp toàn bộ 7 Part trong hệ thống sắp xếp theo ID tăng dần
        List<Part> allParts = partRepo.findAll(Sort.by("id").ascending());

        // Bước 3: Nạp danh sách ngưỡng đã lưu trong DB của AIM này
        List<TargetPartThreshold> existingThresholds = targetPartThresholdRepo.findByTargetProfileId(targetProfileId);
        Map<Long, TargetPartThreshold> thresholdMap = existingThresholds.stream()
                .filter(t -> t.getPart() != null)
                .collect(Collectors.toMap(t -> t.getPart().getId(), t -> t, (a, b) -> a));

        // Bước 4: Tạo cấu trúc các dòng ma trận 7 Part
        List<AimPartThresholdMatrixResponse.PartThresholdRowResponse> rows = new ArrayList<>();
        for (int i = 0; i < allParts.size(); i++) {
            Part part = allParts.get(i);
            TargetPartThreshold threshold = thresholdMap.get(part.getId());

            Float passScore = (threshold != null) ? threshold.getPassScore() : null;
            Float confirmingScore = (threshold != null) ? threshold.getConfirmingScore() : null;
            Long recordId = (threshold != null) ? threshold.getId() : null;

            rows.add(AimPartThresholdMatrixResponse.PartThresholdRowResponse.builder()
                    .id(recordId)
                    .partId(part.getId())
                    .partName(part.getName())
                    .partNo(i + 1)
                    .section(part.getSections() != null ? part.getSections().name() : null)
                    .passScore(passScore)
                    .confirmingScore(confirmingScore)
                    .build());
        }

        return AimPartThresholdMatrixResponse.builder()
                .targetProfileId(targetProfile.getId())
                .aimScore(targetProfile.getTargetTotalScore())
                .aimDescription(targetProfile.getDescription())
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public AimPartThresholdMatrixResponse savePartThresholdsBatch(SavePartThresholdsBatchRequest request) {
        // Bước 1: Kiểm tra TargetProfile tồn tại
        TargetProfile targetProfile = findTargetProfileOrThrow(request.getTargetProfileId());

        // Bước 2: Kiểm tra logic toán học trước khi ghi nhận
        ThresholdValidationResponse validation = validatePartThresholds(request);
        if (!validation.isValid()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, String.join("; ", validation.getErrors()));
        }

        // Bước 3: Lưu hàng loạt theo cơ chế Upsert (Cập nhật nếu có, tạo mới nếu chưa)
        Map<Long, Part> partMap = partRepo.findAll().stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        for (SavePartThresholdsBatchRequest.PartThresholdItem item : request.getThresholds()) {
            Part part = partMap.get(item.getPartId());
            if (part == null) {
                throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Part với ID: " + item.getPartId());
            }

            TargetPartThreshold threshold = targetPartThresholdRepo
                    .findByTargetProfileIdAndPartId(targetProfile.getId(), part.getId())
                    .orElseGet(() -> {
                        TargetPartThreshold newThreshold = new TargetPartThreshold();
                        newThreshold.setTargetProfile(targetProfile);
                        newThreshold.setPart(part);
                        return newThreshold;
                    });

            threshold.setPassScore(item.getPassScore());
            threshold.setConfirmingScore(item.getConfirmingScore());
            targetPartThresholdRepo.save(threshold);
        }

        log.info("Đã cập nhật cấu hình ngưỡng 7 Part cho AIM {}", targetProfile.getTargetTotalScore());
        return getPartThresholdMatrix(targetProfile.getId());
    }

    @Override
    public ThresholdValidationResponse validatePartThresholds(SavePartThresholdsBatchRequest request) {
        List<String> errors = new ArrayList<>();
        Map<Long, Part> partMap = partRepo.findAll().stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        if (request.getThresholds() == null || request.getThresholds().isEmpty()) {
            errors.add("Danh sách ngưỡng Part không được để trống");
            return ThresholdValidationResponse.builder().valid(false).errors(errors).build();
        }

        for (SavePartThresholdsBatchRequest.PartThresholdItem item : request.getThresholds()) {
            Part part = partMap.get(item.getPartId());
            String partLabel = (part != null) ? part.getName() : ("Part ID " + item.getPartId());

            if (item.getConfirmingScore() == null || item.getPassScore() == null) {
                errors.add(partLabel + ": Vui lòng nhập đầy đủ cả 2 mốc điểm (Confirm và Pass)");
                continue;
            }

            if (item.getConfirmingScore() < 0 || item.getConfirmingScore() > 100) {
                errors.add(partLabel + ": Ngưỡng Cận chuẩn phải nằm trong khoảng 0% - 100%");
            }

            if (item.getPassScore() < 0 || item.getPassScore() > 100) {
                errors.add(partLabel + ": Ngưỡng Đạt chuẩn phải nằm trong khoảng 0% - 100%");
            }

            // QUY TẮC TOÁN HỌC CỐT LÕI: confirmingScore < passScore
            if (item.getConfirmingScore() >= item.getPassScore()) {
                errors.add(partLabel + ": Ngưỡng Cận chuẩn (" + item.getConfirmingScore()
                        + "%) phải nhỏ hơn Ngưỡng Đạt chuẩn (" + item.getPassScore() + "%)");
            }
        }

        return ThresholdValidationResponse.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    // =========================================================================
    // A10: CẤU HÌNH QUY TẮC ĐÁNH GIÁ NĂNG LỰC THEO AIM
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AimAbilityRuleMatrixResponse getAbilityRuleMatrix(Long targetProfileId) {
        TargetProfile targetProfile = findTargetProfileOrThrow(targetProfileId);

        // Nạp toàn bộ danh mục Ability trong hệ thống
        List<Ability> allAbilities = abilityRepo.findAll(
                Sort.by("part.id").ascending().and(Sort.by("id").ascending())
        );

        // Nạp các rule đã lưu cho AIM này
        List<AbilityEvaluationRule> existingRules = abilityRuleRepo.findByTargetProfileId(targetProfileId);
        Map<Long, AbilityEvaluationRule> ruleMap = existingRules.stream()
                .filter(r -> r.getAbility() != null)
                .collect(Collectors.toMap(r -> r.getAbility().getId(), r -> r, (a, b) -> a));

        List<AimAbilityRuleMatrixResponse.AbilityRuleRowResponse> rows = new ArrayList<>();
        for (Ability ability : allAbilities) {
            AbilityEvaluationRule rule = ruleMap.get(ability.getId());
            Float stableMin = (rule != null) ? rule.getStableMin() : null;
            Long recordId = (rule != null) ? rule.getId() : null;

            Part part = ability.getPart();
            rows.add(AimAbilityRuleMatrixResponse.AbilityRuleRowResponse.builder()
                    .id(recordId)
                    .abilityId(ability.getId())
                    .abilityName(ability.getName())
                    .abilityDescription(ability.getDescription())
                    .partId(part != null ? part.getId() : null)
                    .partName(part != null ? part.getName() : null)
                    .section(ability.getSections() != null ? ability.getSections().name() : null)
                    .stableMin(stableMin)
                    .build());
        }

        return AimAbilityRuleMatrixResponse.builder()
                .targetProfileId(targetProfile.getId())
                .aimScore(targetProfile.getTargetTotalScore())
                .aimDescription(targetProfile.getDescription())
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public AimAbilityRuleMatrixResponse saveAbilityRulesBatch(SaveAbilityRulesBatchRequest request) {
        TargetProfile targetProfile = findTargetProfileOrThrow(request.getTargetProfileId());

        ThresholdValidationResponse validation = validateAbilityRules(request);
        if (!validation.isValid()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, String.join("; ", validation.getErrors()));
        }

        Map<Long, Ability> abilityMap = abilityRepo.findAll().stream()
                .collect(Collectors.toMap(Ability::getId, a -> a));

        for (SaveAbilityRulesBatchRequest.AbilityRuleItem item : request.getRules()) {
            Ability ability = abilityMap.get(item.getAbilityId());
            if (ability == null) {
                throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Ability với ID: " + item.getAbilityId());
            }

            AbilityEvaluationRule rule = abilityRuleRepo
                    .findByAbilityIdAndTargetProfileId(ability.getId(), targetProfile.getId())
                    .orElseGet(() -> {
                        AbilityEvaluationRule newRule = new AbilityEvaluationRule();
                        newRule.setTargetProfile(targetProfile);
                        newRule.setAbility(ability);
                        return newRule;
                    });

            rule.setStableMin(item.getStableMin());
            abilityRuleRepo.save(rule);
        }

        log.info("Đã cập nhật quy tắc Năng lực cho AIM {}", targetProfile.getTargetTotalScore());
        return getAbilityRuleMatrix(targetProfile.getId());
    }

    @Override
    public ThresholdValidationResponse validateAbilityRules(SaveAbilityRulesBatchRequest request) {
        List<String> errors = new ArrayList<>();
        Map<Long, Ability> abilityMap = abilityRepo.findAll().stream()
                .collect(Collectors.toMap(Ability::getId, a -> a));

        if (request.getRules() == null || request.getRules().isEmpty()) {
            errors.add("Danh sách quy tắc Năng lực không được để trống");
            return ThresholdValidationResponse.builder().valid(false).errors(errors).build();
        }

        for (SaveAbilityRulesBatchRequest.AbilityRuleItem item : request.getRules()) {
            Ability ability = abilityMap.get(item.getAbilityId());
            String abilityLabel = (ability != null) ? ability.getName() : ("Ability ID " + item.getAbilityId());

            if (item.getStableMin() == null) {
                errors.add(abilityLabel + ": Vui lòng nhập ngưỡng Ổn định (Stable)");
                continue;
            }

            if (item.getStableMin() < 0 || item.getStableMin() > 100) {
                errors.add(abilityLabel + ": Ngưỡng Ổn định phải nằm trong khoảng 0% - 100%");
            }
        }

        return ThresholdValidationResponse.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    // =========================================================================
    // SAO CHÉP CẤU HÌNH TỪ AIM KHÁC
    // =========================================================================

    @Override
    @Transactional
    public AimPartThresholdMatrixResponse copyThresholdsFromAim(CopyThresholdsFromAimRequest request) {
        if (request.getTargetProfileId().equals(request.getSourceProfileId())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "AIM nguồn và AIM đích không được trùng nhau");
        }

        TargetProfile sourceProfile = findTargetProfileOrThrow(request.getSourceProfileId());
        TargetProfile targetProfile = findTargetProfileOrThrow(request.getTargetProfileId());

        // 1. Sao chép Part Thresholds
        List<TargetPartThreshold> sourceThresholds = targetPartThresholdRepo.findByTargetProfileId(sourceProfile.getId());
        for (TargetPartThreshold src : sourceThresholds) {
            TargetPartThreshold dest = targetPartThresholdRepo
                    .findByTargetProfileIdAndPartId(targetProfile.getId(), src.getPart().getId())
                    .orElseGet(() -> {
                        TargetPartThreshold n = new TargetPartThreshold();
                        n.setTargetProfile(targetProfile);
                        n.setPart(src.getPart());
                        return n;
                    });
            dest.setPassScore(src.getPassScore());
            dest.setConfirmingScore(src.getConfirmingScore());
            targetPartThresholdRepo.save(dest);
        }

        // 2. Sao chép Ability Rules
        List<AbilityEvaluationRule> sourceRules = abilityRuleRepo.findByTargetProfileId(sourceProfile.getId());
        for (AbilityEvaluationRule src : sourceRules) {
            AbilityEvaluationRule dest = abilityRuleRepo
                    .findByAbilityIdAndTargetProfileId(src.getAbility().getId(), targetProfile.getId())
                    .orElseGet(() -> {
                        AbilityEvaluationRule n = new AbilityEvaluationRule();
                        n.setTargetProfile(targetProfile);
                        n.setAbility(src.getAbility());
                        return n;
                    });
            dest.setStableMin(src.getStableMin());
            abilityRuleRepo.save(dest);
        }

        log.info("Sao chép thành công cấu hình từ AIM {} sang AIM {}",
                sourceProfile.getTargetTotalScore(), targetProfile.getTargetTotalScore());

        return getPartThresholdMatrix(targetProfile.getId());
    }

    private TargetProfile findTargetProfileOrThrow(Long id) {
        return targetProfileRepo.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Mục tiêu AIM với ID: " + id));
    }
}

