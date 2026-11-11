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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        // Bước 1: Lấy danh sách module kèm abilities và part trong 1 query (triệt tiêu N+1)
        List<Module> modules = moduleRepository.findAllWithLessonsAndAbilities();

        // Bước 2: Lấy trực tiếp lessons từ module.
        // Nhờ @BatchSize(size = 50) và default_batch_fetch_size, Hibernate sẽ tự động gom nạp các Lesson
        // của toàn bộ modules theo lô (WHERE module_id IN (...)) và đã được sắp xếp qua @OrderBy("orderNo ASC").
        return modules.stream()
                .map(module -> moduleMapper.toSummaryResponse(module, module.getLessons()))
                .collect(Collectors.toList());
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

        // Bước 4: Liên kết danh sách Ability mục tiêu trước khi lưu
        assignAbilitiesToModule(module, request.getAbilityIds());

        // Bước 5: Lưu Module vào CSDL để sinh ID (CascadeType.ALL tự động lưu moduleAbilities)
        Module savedModule = moduleRepository.save(module);

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

        // Bước 4: Cập nhật quan hệ Many-to-Many với Ability qua collection quản lý của Module
        // orphanRemoval = true sẽ tự động sinh lệnh DELETE các liên kết cũ và INSERT liên kết mới
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
        // Bước 1: Nạp tất cả modules cần đổi thứ tự trong 1 câu SELECT WHERE id IN (...)
        List<Module> modules = moduleRepository.findAllById(orderedIds);
        Map<Long, Module> moduleMap = modules.stream()
                .collect(Collectors.toMap(Module::getId, m -> m));

        // Bước 2: Cập nhật giá trị sequence mới trực tiếp trên Managed Entities
        for (int i = 0; i < orderedIds.size(); i++) {
            Module module = moduleMap.get(orderedIds.get(i));
            if (module != null) {
                module.setSequence(i + 1);
            }
        }

        // TẬN DỤNG DIRTY CHECKING & JDBC BATCHING:
        // Không cần gọi moduleRepository.saveAll(modules). Khi @Transactional commit,
        // Hibernate tự động Dirty Check và nhờ cấu hình jdbc.batch_size + rewriteBatchedStatements,
        // toàn bộ các câu UPDATE sequence sẽ được gom thành 1 chuyến xe JDBC duy nhất gửi xuống MySQL.
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Module findModuleByIdOrThrow(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + id));
    }

    private void assignAbilitiesToModule(Module module, List<Long> abilityIds) {
        if (module.getModuleAbilities() == null) {
            module.setModuleAbilities(new ArrayList<>());
        } else {
            module.getModuleAbilities().clear();
        }

        if (abilityIds == null || abilityIds.isEmpty()) {
            return;
        }

        for (Long abilityId : abilityIds) {
            // NOTE: Ném exception thay vì bỏ qua để tránh lưu Module với danh sách Ability thiếu — dễ gây nhầm lẫn cho admin
            Ability ability = abilityRepository.findById(abilityId)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy Ability với ID: " + abilityId));
            ModuleAbility moduleAbility = new ModuleAbility();
            moduleAbility.setModule(module);
            moduleAbility.setAbility(ability);
            module.getModuleAbilities().add(moduleAbility);
        }
    }
}
