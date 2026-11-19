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

/**
 * Service triển khai quản lý và xử lý cấu hình Ma trận Điểm đỗ (Pass Score) của Module theo từng Mục tiêu AIM (Target Profile).
 * Đóng vai trò là tầng nghiệp vụ phục vụ giao diện bảng quản trị Admin cấu hình ngưỡng đỗ cho học viên.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleTargetThresholdServiceImpl implements ModuleTargetThresholdService {

    private final ModuleTargetThresholdRepository thresholdRepo;
    private final ModuleRepository moduleRepo;
    private final TargetProfileRepository profileRepo;
    private final ModuleTargetThresholdMapper thresholdMapper;

    /**
     * Mức điểm đỗ mặc định toàn hệ thống (65 điểm) dùng làm phương án fallback cuối cùng
     * khi cả bảng ModuleTargetThreshold lẫn thuộc tính Module.defaultPassScore đều bị bỏ trống.
     */
    private static final int SYSTEM_DEFAULT_PASS_SCORE = 65;

    /**
     * Lấy toàn bộ dữ liệu cấu trúc ma trận 2 chiều (Module x Target Profile) để hiển thị bảng cấu hình trên UI Admin.
     * Áp dụng kỹ thuật Batch Fetching để gom toàn bộ dữ liệu chỉ trong 3 câu query, triệt tiêu hoàn toàn lỗi N+1 Query.
     *
     * @param partId ID của Part cần lọc module (truyền null nếu muốn lấy toàn bộ module của 7 Part)
     * @return ModuleThresholdMatrixResponse Cấu trúc ma trận hoàn chỉnh gồm danh sách cột (AIMs) và các hàng (Modules)
     */
    @Override
    @Transactional(readOnly = true)
    public ModuleThresholdMatrixResponse getMatrix(Long partId) {
        log.info("Lấy ma trận ngưỡng điểm đỗ Module cho partId: {}", partId);

        // Bước 1: Lấy danh sách các cột (Target Profile / AIM) sắp xếp theo điểm mục tiêu từ thấp đến cao (ví dụ: 550 -> 990)
        List<TargetProfile> profiles = profileRepo.findAllByOrderByTargetTotalScoreAsc();
        List<Long> profileIds = profiles.stream().map(TargetProfile::getId).toList();
        List<Integer> profileScores = profiles.stream().map(TargetProfile::getScore).toList();
        List<String> profileNames = profiles.stream().map(TargetProfile::getName).toList();

        // Bước 2: Lấy danh sách các hàng (Modules) theo đúng thứ tự sư phạm sequence
        List<Module> modules = (partId != null)
                ? moduleRepo.findByPartIdOrderBySequenceAsc(partId)
                : moduleRepo.findAllByOrderBySequenceAsc();

        // Xử lý sớm (Early Return): Nếu không có Module nào thì trả về ma trận rỗng ngay lập tức
        if (modules.isEmpty()) {
            return ModuleThresholdMatrixResponse.builder()
                    .profileIds(profileIds)
                    .profileScores(profileScores)
                    .profileNames(profileNames)
                    .rows(List.of())
                    .build();
        }

        // Bước 3: Triệt tiêu N+1 Query - Dùng 1 câu truy vấn IN (...) để lấy toàn bộ bản ghi ngưỡng điểm đã được lưu trong DB
        List<Long> moduleIds = modules.stream().map(Module::getId).toList();
        List<ModuleTargetThreshold> existingThresholds = thresholdRepo.findByModuleIdIn(moduleIds);

        // Tạo Lookup Map với key dạng "moduleId_profileId" giúp tra cứu tức thì O(1) trong RAM, tránh query DB lặp lại trong vòng for
        Map<String, ModuleTargetThreshold> thresholdLookup = new HashMap<>();
        for (ModuleTargetThreshold mtt : existingThresholds) {
            String key = mtt.getModule().getId() + "_" + mtt.getTargetProfile().getId();
            thresholdLookup.put(key, mtt);
        }

        // Bước 4: Lắp ráp dữ liệu từng ô giao nhau giữa Module (hàng) và Target Profile (cột)
        List<ModuleThresholdMatrixRowResponse> rows = new ArrayList<>(modules.size());
        for (Module mod : modules) {
            // Xác định điểm mặc định của Module (ưu tiên lấy cấu hình riêng của Module, nếu không có thì lấy hằng số 65 của hệ thống)
            int effectiveDefaultScore = mod.getDefaultPassScore() != null
                    ? mod.getDefaultPassScore()
                    : SYSTEM_DEFAULT_PASS_SCORE;

            Map<String, Integer> scoreMap = new HashMap<>();
            Map<String, Boolean> isCustomMap = new HashMap<>();

            for (TargetProfile profile : profiles) {
                String profileKey = profile.getId().toString();
                String lookupKey = mod.getId() + "_" + profile.getId();
                ModuleTargetThreshold custom = thresholdLookup.get(lookupKey);

                // Nếu trong DB đã cấu hình riêng ô này -> lấy điểm tùy biến và đánh dấu isCustom = true
                if (custom != null && custom.getPassScore() != null) {
                    scoreMap.put(profileKey, custom.getPassScore());
                    isCustomMap.put(profileKey, true);
                } else {
                    // Nếu chưa có cấu hình riêng -> lấy điểm mặc định kế thừa và đánh dấu isCustom = false
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

    /**
     * Cập nhật hàng loạt (Batch Upsert) nhiều ô điểm đỗ cùng lúc khi Admin bấm nút "Lưu thay đổi" trên giao diện ma trận.
     *
     * @param request Chứa danh sách các ô (ModuleId, TargetProfileId, passScore) cần cập nhật
     * @return Danh sách các bản ghi ModuleTargetThreshold sau khi lưu
     */
    @Override
    @Transactional
    public List<ModuleTargetThresholdResponse> batchUpsert(BatchUpdateModuleThresholdRequest request) {
        log.info("Thực hiện batch upsert cho {} mục ngưỡng điểm đỗ Module", request.getItems().size());
        List<ModuleTargetThresholdResponse> results = new ArrayList<>();

        for (ModuleThresholdItemDto item : request.getItems()) {
            if (item.getPassScore() == null) {
                // QUY TẮC NGHIỆP VỤ: Nếu Admin xóa trắng ô điểm (passScore = null), hệ thống tự động xóa bản ghi ghi đè
                // để ô đó quay trở về kế thừa điểm mặc định của Module
                thresholdRepo.deleteByModuleIdAndTargetProfileId(item.getModuleId(), item.getTargetProfileId());
            } else {
                Optional<ModuleTargetThreshold> existingOpt = thresholdRepo.findByModuleIdAndTargetProfileId(
                        item.getModuleId(), item.getTargetProfileId());

                ModuleTargetThreshold saved;
                if (existingOpt.isPresent()) {
                    // Cập nhật lại bản ghi cấu hình đã tồn tại
                    ModuleTargetThreshold existing = existingOpt.get();
                    existing.setPassScore(item.getPassScore());
                    saved = thresholdRepo.save(existing);
                } else {
                    // Tạo mới cấu hình ghi đè nếu trước đó ô này chưa từng được lưu
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

    /**
     * Thêm mới hoặc cập nhật trực tiếp điểm đỗ cho một ô duy nhất (Single Upsert).
     *
     * @param moduleId ID của Module cần cấu hình
     * @param targetProfileId ID của Mục tiêu AIM cần áp dụng
     * @param passScore Mức điểm đỗ (từ 0 đến 100), nếu null sẽ tự động reset về điểm mặc định
     * @return ModuleTargetThresholdResponse Bản ghi sau khi cập nhật (hoặc null nếu đã xóa về mặc định)
     * @throws BaseException Nếu điểm số nằm ngoài phạm vi hợp lệ [0, 100] hoặc không tìm thấy Entity liên quan
     */
    @Override
    @Transactional
    public ModuleTargetThresholdResponse upsertSingle(Long moduleId, Long targetProfileId, Integer passScore) {
        // Kiểm tra hợp lệ dữ liệu đầu vào: Điểm số TOEIC theo phần trăm phải nằm trong đoạn [0, 100]
        if (passScore != null && (passScore < 0 || passScore > 100)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Điểm đỗ phải nằm trong khoảng từ 0 đến 100");
        }

        // Nếu truyền passScore null -> reset về giá trị mặc định của Module
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

    /**
     * Xóa bỏ cấu hình điểm đỗ riêng của một ô (Module x Target Profile), đưa ô đó về trạng thái kế thừa mặc định.
     *
     * @param moduleId ID của Module
     * @param targetProfileId ID của Target Profile
     */
    @Override
    @Transactional
    public void resetToDefault(Long moduleId, Long targetProfileId) {
        log.info("Reset điểm đỗ Module {} tại Profile {} về mặc định", moduleId, targetProfileId);
        thresholdRepo.deleteByModuleIdAndTargetProfileId(moduleId, targetProfileId);
    }
}
