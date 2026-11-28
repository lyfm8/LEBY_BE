package com.example.be.service.content.implement;

import com.example.be.dto.response.content.PublicCatalogModuleResponse;
import com.example.be.dto.response.content.PublicCatalogPartResponse;
import com.example.be.dto.response.content.PublicCatalogResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.mapper.content.PublicCatalogMapper;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.service.content.service.PublicCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Triển khai dịch vụ lấy danh mục chương trình 7 Part TOEIC công khai cho Landing Page
 * bằng truy vấn batch O(1), không chứa dữ liệu cá nhân hay URL học liệu nội bộ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicCatalogServiceImpl implements PublicCatalogService {

    private final PartRepository partRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final ModuleTestQuestionRepository moduleTestQuestionRepository;
    private final PublicCatalogMapper publicCatalogMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PublicCatalogResponse getPublicCatalog() {
        List<Part> allParts = partRepository.findAllByOrderByIdAsc();
        if (allParts.isEmpty()) {
            log.warn("[PublicCatalog] Chưa có dữ liệu Part trong cơ sở dữ liệu");
            return publicCatalogMapper.toCatalogResponse(Collections.emptyList());
        }

        List<Module> publishedModules = moduleRepository
                .findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED);

        List<Long> moduleIds = publishedModules.stream()
                .map(Module::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> videoCountMap = Collections.emptyMap();
        Map<Long, Integer> practiceCountMap = Collections.emptyMap();
        Map<Long, Integer> manualTestQuestionMap = Collections.emptyMap();

        if (!moduleIds.isEmpty()) {
            videoCountMap = toCountMap(lessonRepository.countByModuleIdInAndStatusAndLessonType(
                    moduleIds,
                    EModuleStatus.PUBLISHED,
                    ELessonType.VIDEO
            ));
            practiceCountMap = toCountMap(lessonRepository.countByModuleIdInAndStatusAndLessonType(
                    moduleIds,
                    EModuleStatus.PUBLISHED,
                    ELessonType.PRACTICE
            ));
            manualTestQuestionMap = toCountMap(
                    moduleTestQuestionRepository.countQuestionsGroupedByModuleIds(moduleIds)
            );
        }

        Map<Long, List<PublicCatalogModuleResponse>> modulesByPartId = new LinkedHashMap<>();
        for (Module module : publishedModules) {
            if (module.getPart() == null || module.getPart().getId() == null) {
                continue;
            }
            Long moduleId = module.getId();
            int videoCount = moduleId != null ? videoCountMap.getOrDefault(moduleId, 0) : 0;
            int practiceCount = moduleId != null ? practiceCountMap.getOrDefault(moduleId, 0) : 0;
            int manualQuestionCount = moduleId != null ? manualTestQuestionMap.getOrDefault(moduleId, 0) : 0;

            PublicCatalogModuleResponse mappedModule = publicCatalogMapper.toModuleResponse(
                    module,
                    videoCount,
                    practiceCount,
                    manualQuestionCount
            );
            modulesByPartId
                    .computeIfAbsent(module.getPart().getId(), ignored -> new ArrayList<>())
                    .add(mappedModule);
        }

        List<PublicCatalogPartResponse> partResponses = new ArrayList<>(allParts.size());
        for (Part part : allParts) {
            List<PublicCatalogModuleResponse> partModules = modulesByPartId.getOrDefault(
                    part.getId(),
                    Collections.emptyList()
            );
            partResponses.add(publicCatalogMapper.toPartResponse(part, partModules));
        }

        return publicCatalogMapper.toCatalogResponse(partResponses);
    }

    private Map<Long, Integer> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Long id = ((Number) row[0]).longValue();
                int count = row[1] != null ? ((Number) row[1]).intValue() : 0;
                result.put(id, count);
            }
        }
        return result;
    }
}
