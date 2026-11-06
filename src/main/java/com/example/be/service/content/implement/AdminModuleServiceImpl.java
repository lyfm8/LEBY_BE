package com.example.be.service.content.implement;

import com.example.be.dto.request.content.CreateModuleRequest;
import com.example.be.dto.request.content.ReorderModulesRequest;
import com.example.be.dto.request.content.UpdateModuleRequest;
import com.example.be.dto.request.content.UpdateModuleThresholdsRequest;
import com.example.be.dto.response.content.ModuleDetailResponse;
import com.example.be.dto.response.content.ModuleSummaryResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.ModuleMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleAbilityRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.service.content.service.AdminModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý Module học tập dành cho Quản trị viên và Giáo viên.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminModuleServiceImpl implements AdminModuleService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final PartRepository partRepository;
    private final AbilityRepository abilityRepository;
    private final ModuleAbilityRepository moduleAbilityRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final ModuleMapper moduleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleSummaryResponse> getAllModules() {
        List<Module> modules = moduleRepository.findAllByOrderBySequenceAsc();
        return modules.stream().map(module -> {
            List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderNoAsc(module.getId());
            return moduleMapper.toSummaryResponse(module, lessons);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleDetailResponse getModuleById(Long id) {
        Module module = findModuleByIdOrThrow(id);
        long lessonCount = lessonRepository.countByModuleId(module.getId());
        return moduleMapper.toDetailResponse(module, lessonCount);
    }

    @Override
    @Transactional
    public ModuleDetailResponse createModule(CreateModuleRequest request) {
        // Bước 1: Tính sequence nếu chưa truyền
        if (request.getSequence() == null) {
            int nextSeq = moduleRepository.findMaxSequence().orElse(0) + 1;
            request.setSequence(nextSeq);
        }

        // Bước 2: Liên kết với Part nếu có
        Part part = null;
        if (request.getPartId() != null) {
            part = partRepository.findById(request.getPartId())
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Part với ID: " + request.getPartId()));
        }

        // Bước 3: Sử dụng ModuleMapper để chuyển đổi DTO sang Entity
        Module module = moduleMapper.toEntity(request, part);

        // Bước 4: Lưu Module vào CSDL để sinh ID
        Module savedModule = moduleRepository.save(module);

        // Bước 5: Liên kết danh sách Ability mục tiêu
        assignAbilitiesToModule(savedModule, request.getAbilityIds());

        return getModuleById(savedModule.getId());
    }

    @Override
    @Transactional
    public ModuleDetailResponse updateModule(Long id, UpdateModuleRequest request) {
        // Bước 1: Kiểm tra sự tồn tại của Module
        Module module = findModuleByIdOrThrow(id);

        // Bước 2: Tìm Part nếu có
        Part part = null;
        if (request.getPartId() != null) {
            part = partRepository.findById(request.getPartId())
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Part với ID: " + request.getPartId()));
        }

        // Bước 3: Cập nhật Entity thông qua ModuleMapper
        moduleMapper.updateEntity(module, request, part);

        // Bước 4: Cập nhật quan hệ Many-to-Many với Ability
        moduleAbilityRepository.deleteByModuleId(module.getId());
        assignAbilitiesToModule(module, request.getAbilityIds());

        moduleRepository.save(module);
        return getModuleById(id);
    }

    @Override
    @Transactional
    public ModuleDetailResponse updateModuleThresholds(Long id, UpdateModuleThresholdsRequest request) {
        Module module = findModuleByIdOrThrow(id);
        module.setDefaultPassScore(request.getDefaultPassScore());
        module.setRequiredCompletionRate(request.getRequiredCompletionRate());

        moduleRepository.save(module);
        return getModuleById(id);
    }

    @Override
    @Transactional
    public void deleteModule(Long id) {
        Module module = findModuleByIdOrThrow(id);

        // QUY TẮC NGHIỆP VỤ BẢO VỆ TIẾN TRÌNH HỌC (Luồng ngoại lệ 2c - Use Case A05):
        // Nếu Module đang nằm trong lộ trình của bất kỳ học viên nào, chặn xóa trực tiếp để tránh làm đứt gãy tiến độ.
        boolean hasActiveLearners = learningPathItemRepository.existsByModuleId(id);
        if (hasActiveLearners) {
            log.warn("Chặn xóa Module ID: {} do đang có học viên theo học trong lộ trình", id);
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Mô-đun này hiện đang có học viên theo học trên lộ trình. Bạn không thể xóa để tránh làm đứt gãy tiến độ học tập. Bạn có thể chọn chế độ Tạm ẩn mô-đun."
            );
        }

        moduleRepository.delete(module);
    }

    @Override
    @Transactional
    public void reorderModules(ReorderModulesRequest request) {
        List<Long> orderedIds = request.getOrderedModuleIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long moduleId = orderedIds.get(i);
            int newSequence = i + 1;
            Module module = moduleRepository.findById(moduleId).orElse(null);
            if (module != null) {
                module.setSequence(newSequence);
                moduleRepository.save(module);
            }
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Module findModuleByIdOrThrow(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + id));
    }

    private void assignAbilitiesToModule(Module module, List<Long> abilityIds) {
        if (abilityIds == null || abilityIds.isEmpty()) {
            return;
        }

        for (Long abilityId : abilityIds) {
            Ability ability = abilityRepository.findById(abilityId).orElse(null);
            if (ability != null) {
                ModuleAbility moduleAbility = new ModuleAbility();
                moduleAbility.setModule(module);
                moduleAbility.setAbility(ability);
                moduleAbilityRepository.save(moduleAbility);
            }
        }
    }
}
