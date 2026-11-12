package com.example.be.mapper.content;

import com.example.be.dto.request.content.CreateModuleRequest;
import com.example.be.dto.request.content.UpdateModuleRequest;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.dto.response.content.ModuleDetailResponse;
import com.example.be.dto.response.content.ModuleSummaryResponse;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.enums.content.EModuleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi giữa Entity Module và các DTO Request/Response (Use Case A05).
 */
@Component
@RequiredArgsConstructor
public class ModuleMapper {

    private final LessonMapper lessonMapper;

    /**
     * Chuyển đổi CreateModuleRequest sang JPA Entity Module.
     */
    public Module toEntity(CreateModuleRequest request, Part part) {
        if (request == null) {
            return null;
        }

        Module module = new Module();
        module.setTitle(request.getTitle().trim());
        module.setType(request.getType());
        module.setSequence(request.getSequence());
        module.setStatus(request.getStatus() != null ? request.getStatus() : EModuleStatus.DRAFT);
        module.setDefaultPassScore(request.getDefaultPassScore() != null ? request.getDefaultPassScore() : 65);
        module.setRequiredCompletionRate(request.getRequiredCompletionRate() != null ? request.getRequiredCompletionRate() : 80);
        module.setDescriptions(request.getDescriptions());
        module.setPart(part);

        return module;
    }

    /**
     * Cập nhật Entity Module từ UpdateModuleRequest.
     */
    public void updateEntity(Module module, UpdateModuleRequest request, Part part) {
        if (module == null || request == null) {
            return;
        }

        module.setTitle(request.getTitle().trim());
        module.setType(request.getType());
        module.setSequence(request.getSequence());
        if (request.getStatus() != null) {
            module.setStatus(request.getStatus());
        }
        if (request.getDefaultPassScore() != null) {
            module.setDefaultPassScore(request.getDefaultPassScore());
        }
        if (request.getRequiredCompletionRate() != null) {
            module.setRequiredCompletionRate(request.getRequiredCompletionRate());
        }
        module.setDescriptions(request.getDescriptions());
        module.setPart(part);
    }

    /**
     * Chuyển đổi Module sang ModuleSummaryResponse (dùng cho cây danh mục 25%).
     */
    public ModuleSummaryResponse toSummaryResponse(Module module, List<Lesson> lessons) {
        if (module == null) {
            return null;
        }

        List<LessonSummaryResponse> lessonResponses = lessons != null
                ? lessons.stream().map(lessonMapper::toSummaryResponse).collect(Collectors.toList())
                : Collections.emptyList();

        int totalVideo = 0;
        int totalPractice = 0;
        if (lessons != null) {
            for (Lesson l : lessons) {
                if (l.getVideoUrl() != null && !l.getVideoUrl().isBlank()) {
                    totalVideo++;
                } else {
                    totalPractice++;
                }
            }
        }

        List<Long> abilityIds = new ArrayList<>();
        List<String> abilityNames = new ArrayList<>();
        if (module.getModuleAbilities() != null) {
            for (ModuleAbility ma : module.getModuleAbilities()) {
                if (ma.getAbility() != null) {
                    abilityIds.add(ma.getAbility().getId());
                    abilityNames.add(ma.getAbility().getName());
                }
            }
        }

        return ModuleSummaryResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .type(module.getType())
                .sequence(module.getSequence())
                .status(module.getStatus())
                .defaultPassScore(module.getDefaultPassScore())
                .requiredCompletionRate(module.getRequiredCompletionRate())
                .partId(module.getPart() != null ? module.getPart().getId() : null)
                .partName(module.getPart() != null ? module.getPart().getName() : null)
                .abilityIds(abilityIds)
                .abilityNames(abilityNames)
                .totalLessons(lessonResponses.size())
                .totalVideoLessons(totalVideo)
                .totalPracticeLessons(totalPractice)
                .lessons(lessonResponses)
                .build();
    }

    /**
     * Chuyển đổi Module sang ModuleDetailResponse (dùng cho Editor xem chi tiết bên phải).
     */
    public ModuleDetailResponse toDetailResponse(Module module, long lessonCount) {
        if (module == null) {
            return null;
        }

        int totalVideo = 0;
        int totalPractice = 0;
        if (module.getLessons() != null) {
            for (Lesson l : module.getLessons()) {
                if (l.getVideoUrl() != null && !l.getVideoUrl().isBlank()) {
                    totalVideo++;
                } else {
                    totalPractice++;
                }
            }
        }

        List<Long> abilityIds = new ArrayList<>();
        List<String> abilityNames = new ArrayList<>();
        if (module.getModuleAbilities() != null) {
            for (ModuleAbility ma : module.getModuleAbilities()) {
                if (ma.getAbility() != null) {
                    abilityIds.add(ma.getAbility().getId());
                    abilityNames.add(ma.getAbility().getName());
                }
            }
        }

        return ModuleDetailResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .type(module.getType())
                .sequence(module.getSequence())
                .status(module.getStatus())
                .defaultPassScore(module.getDefaultPassScore())
                .requiredCompletionRate(module.getRequiredCompletionRate())
                .descriptions(module.getDescriptions())
                .partId(module.getPart() != null ? module.getPart().getId() : null)
                .partName(module.getPart() != null ? module.getPart().getName() : null)
                .abilityIds(abilityIds)
                .abilityNames(abilityNames)
                .totalLessons((int) lessonCount)
                .totalVideoLessons(totalVideo)
                .totalPracticeLessons(totalPractice)
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }
}
