package com.example.be.service.setting.implement;

import com.example.be.dto.request.setting.BatchUpdateModuleThresholdRequest;
import com.example.be.dto.request.setting.ModuleThresholdItemDto;
import com.example.be.dto.response.setting.ModuleTargetThresholdResponse;
import com.example.be.dto.response.setting.ModuleThresholdMatrixResponse;
import com.example.be.dto.response.setting.ModuleThresholdMatrixRowResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.setting.ModuleTargetThresholdMapper;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.repository.user.TargetProfileRepository;
import com.example.be.service.setting.service.ModuleTargetThresholdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleTargetThresholdServiceImpl implements ModuleTargetThresholdService {

    private final ModuleTargetThresholdRepository thresholdRepo;
    private final ModuleRepository moduleRepo;
    private final TargetProfileRepository profileRepo;
    private final ModuleTargetThresholdMapper thresholdMapper;

    private static final int SYSTEM_DEFAULT_PASS_SCORE = 65;

    @Override
    @Transactional(readOnly = true)
    public ModuleThresholdMatrixResponse getMatrix(Long partId) {
        log.info("Lấy ma trận ngưỡng điểm đỗ Module cho partId: {}", partId);

        // Bước 1: Lấy danh sách Profile AIM sắp xếp theo điểm mục tiêu tăng dần
        List<TargetProfile> profiles = profileRepo.findAllByOrderByTargetTotalScoreAsc();
        List<Long> profileIds = profiles.stream().map(TargetProfile::getId).toList();
        List<Integer> profileScores = profiles.stream().map(TargetProfile::getScore).toList();
        List<String> profileNames = profiles.stream().map(TargetProfile::getName).toList();

        // Bước 2: Lấy danh sách Modules theo Part hoặc toàn bộ
        List<Module> modules = (partId != null)
                ? moduleRepo.findByPartIdOrderBySequenceAsc(partId)
                : moduleRepo.findAllByOrderBySequenceAsc();

        if (modules.isEmpty()) {
            return ModuleThresholdMatrixResponse.builder()
                    .profileIds(profileIds)
                    .profileScores(profileScores)
                    .profileNames(profileNames)
                    .rows(List.of())
                    .build();
        }

        // Bước 3: Tải toàn bộ cấu hình ngưỡng liên quan bằng 1 query duy nhất (triệt tiêu N+1)
        List<Long> moduleIds = modules.stream().map(Module::getId).toList();
        List<ModuleTargetThreshold> existingThresholds = thresholdRepo.findByModuleIdIn(moduleIds);

        Map<String, ModuleTargetThreshold> thresholdLookup = new HashMap<>();
        for (ModuleTargetThreshold mtt : existingThresholds) {
            String key = mtt.getModule().getId() + "_" + mtt.getTargetProfile().getId();
            thresholdLookup.put(key, mtt);
        }

        // Bước 4: Lắp ráp các hàng của ma trận
        List<ModuleThresholdMatrixRowResponse> rows = new ArrayList<>(modules.size());
        for (Module mod : modules) {
            int effectiveDefaultScore = mod.getDefaultPassScore() != null
                    ? mod.getDefaultPassScore()
                    : SYSTEM_DEFAULT_PASS_SCORE;

            Map<String, Integer> scoreMap = new HashMap<>();
            Map<String, Boolean> isCustomMap = new HashMap<>();

            for (TargetProfile profile : profiles) {
                String profileKey = profile.getId().toString();
                String lookupKey = mod.getId() + "_" + profile.getId();
                ModuleTargetThreshold custom = thresholdLookup.get(lookupKey);

                if (custom != null && custom.getPassScore() != null) {
                    scoreMap.put(profileKey, custom.getPassScore());
                    isCustomMap.put(profileKey, true);
                } else {
                    scoreMap.put(profileKey, effectiveDefaultScore);
                    isCustomMap.put(profileKey, false);
                }
            }

            rows.add(ModuleThresholdMatrixRowResponse.builder()
                    .moduleId(mod.getId())
                    .moduleTitle(mod.getTitle())
                    .moduleSequence(mod.getSequence())
                    .partId(mod.getPart() != null ? mod.getPart().getId() : null)
                    .partName(mod.getPart() != null ? mod.getPart().getName() : null)
                    .defaultPassScore(effectiveDefaultScore)
                    .thresholds(scoreMap)
                    .isCustom(isCustomMap)
                    .build());
        }

        return ModuleThresholdMatrixResponse.builder()
                .profileIds(profileIds)
                .profileScores(profileScores)
                .profileNames(profileNames)
                .rows(rows)
                .build();
    }

    @Override
    @Transactional
    public List<ModuleTargetThresholdResponse> batchUpsert(BatchUpdateModuleThresholdRequest request) {
        log.info("Thực hiện batch upsert cho {} mục ngưỡng điểm đỗ Module", request.getItems().size());
        List<ModuleTargetThresholdResponse> results = new ArrayList<>();

        for (ModuleThresholdItemDto item : request.getItems()) {
            if (item.getPassScore() == null) {
                // passScore là null -> reset về giá trị mặc định của module (xóa bản ghi ghi đè)
                thresholdRepo.deleteByModuleIdAndTargetProfileId(item.getModuleId(), item.getTargetProfileId());
            } else {
                Optional<ModuleTargetThreshold> existingOpt = thresholdRepo.findByModuleIdAndTargetProfileId(
                        item.getModuleId(), item.getTargetProfileId());

                ModuleTargetThreshold saved;
                if (existingOpt.isPresent()) {
                    ModuleTargetThreshold existing = existingOpt.get();
                    existing.setPassScore(item.getPassScore());
                    saved = thresholdRepo.save(existing);
                } else {
                    Module module = moduleRepo.findById(item.getModuleId())
                            .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + item.getModuleId()));
                    TargetProfile profile = profileRepo.findById(item.getTargetProfileId())
                            .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Target Profile với ID: " + item.getTargetProfileId()));

                    ModuleTargetThreshold newEntity = thresholdMapper.toEntity(item, module, profile);
                    saved = thresholdRepo.save(newEntity);
                }
                results.add(thresholdMapper.toResponse(saved));
            }
        }

        return results;
    }

    @Override
    @Transactional
    public ModuleTargetThresholdResponse upsertSingle(Long moduleId, Long targetProfileId, Integer passScore) {
        if (passScore == null) {
            thresholdRepo.deleteByModuleIdAndTargetProfileId(moduleId, targetProfileId);
            return null;
        }

        Optional<ModuleTargetThreshold> existingOpt = thresholdRepo.findByModuleIdAndTargetProfileId(moduleId, targetProfileId);
        ModuleTargetThreshold saved;
        if (existingOpt.isPresent()) {
            ModuleTargetThreshold existing = existingOpt.get();
            existing.setPassScore(passScore);
            saved = thresholdRepo.save(existing);
        } else {
            Module module = moduleRepo.findById(moduleId)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + moduleId));
            TargetProfile profile = profileRepo.findById(targetProfileId)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Target Profile với ID: " + targetProfileId));

            ModuleThresholdItemDto dto = ModuleThresholdItemDto.builder()
                    .moduleId(moduleId)
                    .targetProfileId(targetProfileId)
                    .passScore(passScore)
                    .build();
            ModuleTargetThreshold newEntity = thresholdMapper.toEntity(dto, module, profile);
            saved = thresholdRepo.save(newEntity);
        }
        return thresholdMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void resetToDefault(Long moduleId, Long targetProfileId) {
        log.info("Reset điểm đỗ Module {} tại Profile {} về mặc định", moduleId, targetProfileId);
        thresholdRepo.deleteByModuleIdAndTargetProfileId(moduleId, targetProfileId);
    }
}
