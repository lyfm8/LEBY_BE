package com.example.be.service.content.implement;

import com.example.be.dto.response.content.CatalogModuleItemResponse;
import com.example.be.dto.response.content.CatalogPartGroupResponse;
import com.example.be.dto.response.content.CourseCatalogResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.mapper.content.UserCatalogMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.service.content.service.UserCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Triển khai nghiệp vụ Thư viện khóa học 7 Part TOEIC cho học viên (UC17 Catalog).
 * Tối ưu hiệu năng tuyệt đối: toàn bộ 7 Part, Module, Ability, đề cương Lesson và tiến độ học viên
 * được nạp trong tối đa 5–6 câu SQL cố định (O(1) query — triệt tiêu hoàn toàn N+1 và tránh MultipleBagFetchException).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCatalogServiceImpl implements UserCatalogService {

    private static final List<ELearningPathStatus> CURRENT_PATH_STATUSES = List.of(
            ELearningPathStatus.ACTIVE,
            ELearningPathStatus.COMPLETED
    );

    private final PartRepository partRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final UserCatalogMapper userCatalogMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public CourseCatalogResponse getCourseCatalogForUser(Long userId) {
        // Bước 1: Nạp toàn bộ 7 phần thi TOEIC theo thứ tự chuẩn Part 1 -> Part 7 (1 câu SQL)
        List<Part> allParts = partRepository.findAllByOrderByIdAsc();
        Optional<LearningPath> currentPathOpt = resolveCurrentLearningPath(userId);
        LearningPath currentPath = currentPathOpt.orElse(null);

        if (allParts == null || allParts.isEmpty()) {
            log.warn("[UC17-Catalog] Chưa có dữ liệu danh mục Part trong cơ sở dữ liệu");
            return userCatalogMapper.toCourseCatalogResponse(currentPath, Collections.emptyList());
        }

        // Bước 2: Nạp toàn bộ Module đã xuất bản (PUBLISHED) kèm Part và Abilities trong 1 câu SQL duy nhất
        List<Module> publishedModules = moduleRepository
                .findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED);

        // Gom danh sách Module theo partId và thu thập toàn bộ moduleId để nạp dữ liệu con theo lô
        Map<Long, List<Module>> modulesByPartId = new HashMap<>();
        List<Long> allModuleIds = new ArrayList<>();
        if (publishedModules != null) {
            for (Module module : publishedModules) {
                if (module == null || module.getId() == null) {
                    continue;
                }
                allModuleIds.add(module.getId());
                if (module.getPart() != null && module.getPart().getId() != null) {
                    modulesByPartId
                            .computeIfAbsent(module.getPart().getId(), key -> new ArrayList<>())
                            .add(module);
                }
            }
        }

        // Bước 3: Nạp đề cương bài học PUBLISHED cho toàn bộ Module bằng đúng 1 câu IN (guard clause chống IN rỗng)
        Map<Long, List<Lesson>> lessonsByModuleId = loadPublishedLessonsGroupedByModuleId(allModuleIds);

        // Bước 4: Nếu học viên đã có lộ trình hiện hành -> nạp LearningPathItem & tiến độ hoàn thành thật trong 2 câu SQL
        boolean hasLearningPath = currentPath != null;
        Map<Long, LearningPathItem> pathItemByModuleId = hasLearningPath
                ? loadPathItemMapByModuleId(currentPath.getId())
                : Collections.emptyMap();
        Map<Long, Integer> completedLessonsByModuleId = hasLearningPath
                ? loadCompletedLessonCountByModuleIds(userId, allModuleIds)
                : Collections.emptyMap();

        // Bước 5: Duyệt đủ 7 Part và map từng Module kèm phân quyền (Trọng tâm cần học / Học tự do / Xem trước)
        List<CatalogPartGroupResponse> partGroups = new ArrayList<>(allParts.size());
        for (Part part : allParts) {
            List<Module> partModules = modulesByPartId.getOrDefault(part.getId(), Collections.emptyList());
            List<CatalogModuleItemResponse> mappedModules = new ArrayList<>(partModules.size());

            for (Module module : partModules) {
                Long moduleId = module.getId();
                List<Lesson> moduleLessons = lessonsByModuleId.getOrDefault(moduleId, Collections.emptyList());
                LearningPathItem pathItem = pathItemByModuleId.get(moduleId);
                int completedCount = completedLessonsByModuleId.getOrDefault(moduleId, 0);

                mappedModules.add(userCatalogMapper.toCatalogModuleItemResponse(
                        module,
                        moduleLessons,
                        pathItem,
                        hasLearningPath,
                        completedCount
                ));
            }

            partGroups.add(userCatalogMapper.toCatalogPartGroupResponse(part, mappedModules));
        }

        // Bước 6: Tổng hợp toàn bộ Thư viện khóa học 7 Part trả về cho học viên
        return userCatalogMapper.toCourseCatalogResponse(currentPath, partGroups);
    }

    /**
     * Tìm lộ trình hiện hành của học viên: ưu tiên lộ trình đang ACTIVE,
     * fallback sang bản COMPLETED mới nhất (hỗ trợ trường hợp đã hoàn thành 100% hoặc PASS 7 Part).
     */
    private Optional<LearningPath> resolveCurrentLearningPath(Long userId) {
        List<LearningPath> candidates = learningPathRepository
                .findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(userId, CURRENT_PATH_STATUSES);

        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(path -> path.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> Optional.of(candidates.get(0)));
    }

    /**
     * Nạp toàn bộ đề cương bài học PUBLISHED cho tập hợp moduleIds trong 1 câu SQL và gom nhóm theo moduleId.
     */
    private Map<Long, List<Lesson>> loadPublishedLessonsGroupedByModuleId(List<Long> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Lesson> lessons = lessonRepository
                .findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(moduleIds, EModuleStatus.PUBLISHED);
        if (lessons == null || lessons.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<Lesson>> grouped = new HashMap<>();
        for (Lesson lesson : lessons) {
            if (lesson != null && lesson.getModule() != null && lesson.getModule().getId() != null) {
                grouped.computeIfAbsent(lesson.getModule().getId(), key -> new ArrayList<>()).add(lesson);
            }
        }
        return grouped;
    }

    /**
     * Nạp danh sách chặng học của lộ trình hiện hành và lập bảng băm theo moduleId để tra cứu O(1).
     */
    private Map<Long, LearningPathItem> loadPathItemMapByModuleId(Long learningPathId) {
        List<LearningPathItem> items = learningPathItemRepository
                .findByLearningPathIdWithDetailsOrderByOrderNoAsc(learningPathId);
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, LearningPathItem> map = new HashMap<>(items.size());
        for (LearningPathItem item : items) {
            if (item != null && item.getModule() != null && item.getModule().getId() != null) {
                map.putIfAbsent(item.getModule().getId(), item);
            }
        }
        return map;
    }

    /**
     * Đếm số bài học đã xuất bản được hoàn thành thực tế từ bảng lesson_progress trong 1 câu GROUP BY.
     */
    private Map<Long, Integer> loadCompletedLessonCountByModuleIds(Long userId, List<Long> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = lessonProgressRepository.countCompletedLessonsGroupedByModuleId(
                userId,
                moduleIds,
                EModuleStatus.PUBLISHED
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                Long moduleId = ((Number) row[0]).longValue();
                int count = Objects.nonNull(row[1]) ? ((Number) row[1]).intValue() : 0;
                result.put(moduleId, count);
            }
        }
        return result;
    }
}
