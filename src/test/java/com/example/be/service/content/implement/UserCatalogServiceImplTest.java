package com.example.be.service.content.implement;

import com.example.be.dto.response.content.CatalogModuleItemResponse;
import com.example.be.dto.response.content.CatalogPartGroupResponse;
import com.example.be.dto.response.content.CourseCatalogResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.content.Part;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.mapper.content.UserCatalogMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests kiểm thử toàn diện nghiệp vụ UC17 (Catalog — Thư viện khóa học 7 Part TOEIC):
 * - Học viên chưa thi chẩn đoán -> thấy full 7 Part & đề cương ở chế độ PREVIEW_ONLY (canAccess = false)
 * - Học viên có lộ trình ACTIVE -> phân biệt rõ ROADMAP_OPEN, ROADMAP_LOCKED (BR10) và FREE_STUDY ngoài lộ trình
 * - Học viên PASS 100% 7 Part (COMPLETED, 0 chặng) -> toàn bộ 7 Part mở FREE_STUDY
 * - Biên không có mô-đun xuất bản -> không bắn query IN rỗng
 * - Đảm bảo 0 N+1 Query (số lần gọi repository cố định O(1))
 */
@ExtendWith(MockitoExtension.class)
class UserCatalogServiceImplTest {

    private static final Long USER_ID = 15L;
    private static final Long PATH_ID = 200L;

    @Mock
    private PartRepository partRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningPathItemRepository learningPathItemRepository;

    private UserCatalogServiceImpl userCatalogService;

    @BeforeEach
    void setUp() {
        UserCatalogMapper mapper = new UserCatalogMapper();
        userCatalogService = new UserCatalogServiceImpl(
                partRepository,
                moduleRepository,
                lessonRepository,
                lessonProgressRepository,
                learningPathRepository,
                learningPathItemRepository,
                mapper
        );
    }

    @Test
    @DisplayName("UC17 - Chưa thi chẩn đoán: hiển thị đủ Part & đề cương ở chế độ PREVIEW_ONLY, khóa vào học")
    void getCourseCatalogForUser_whenUserHasNoLearningPath_returnsPreviewOnlyMode() {
        Part part1 = buildPart(1L, "Part 1: Mô tả tranh", ESection.LISTENING);
        Part part5 = buildPart(5L, "Part 5: Hoàn thành câu", ESection.READING);

        Module mod1 = buildModule(10L, part1, 1, "Nghe tranh người", List.of("Nghe hành động"));
        Module mod5 = buildModule(50L, part5, 5, "Từ loại & Ngữ pháp", List.of("Danh từ", "Động từ"));

        Lesson lesson1 = buildLesson(101L, mod1, 1, "Chiến thuật nghe tranh", ELessonType.VIDEO, 420);
        Lesson lesson2 = buildLesson(102L, mod1, 2, "Luyện tập tranh người", ELessonType.PRACTICE, 0);

        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1, part5));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(List.of(mod1, mod5));
        when(lessonRepository.findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(
                eq(List.of(10L, 50L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(List.of(lesson1, lesson2));

        CourseCatalogResponse result = userCatalogService.getCourseCatalogForUser(USER_ID);

        assertThat(result.getHasLearningPath()).isFalse();
        assertThat(result.getTotalParts()).isEqualTo(2);
        assertThat(result.getTotalModules()).isEqualTo(2);
        assertThat(result.getTotalLessons()).isEqualTo(2);
        assertThat(result.getRoadmapModuleCount()).isZero();
        assertThat(result.getFreeStudyModuleCount()).isZero();

        CatalogPartGroupResponse group1 = result.getParts().get(0);
        assertThat(group1.getModules()).hasSize(1);
        CatalogModuleItemResponse mod1Dto = group1.getModules().get(0);
        assertThat(mod1Dto.getAccessMode()).isEqualTo(ECatalogAccessMode.PREVIEW_ONLY);
        assertThat(mod1Dto.getCanAccess()).isFalse();
        assertThat(mod1Dto.getInRoadmap()).isFalse();
        assertThat(mod1Dto.getLockReason()).isNotBlank();
        assertThat(mod1Dto.getVideoLessonCount()).isEqualTo(1);
        assertThat(mod1Dto.getPracticeLessonCount()).isEqualTo(1);
        assertThat(mod1Dto.getTotalDurationSeconds()).isEqualTo(420);
        assertThat(mod1Dto.getLessonPreviews()).hasSize(2);
        assertThat(mod1Dto.getAbilities()).containsExactly("Nghe hành động");

        // Không truy vấn lộ trình con hay lesson_progress khi user chưa thi chẩn đoán
        verify(learningPathItemRepository, never()).findByLearningPathIdWithDetailsOrderByOrderNoAsc(PATH_ID);
        verify(lessonProgressRepository, never()).countCompletedLessonsGroupedByModuleId(
                eq(USER_ID), anyCollection(), eq(EModuleStatus.PUBLISHED));
    }

    @Test
    @DisplayName("UC17 - Có lộ trình ACTIVE: phân tách chuẩn ROADMAP_OPEN, ROADMAP_LOCKED (BR10) và FREE_STUDY")
    void getCourseCatalogForUser_whenUserHasActiveRoadmap_distinguishesRoadmapAndFreeStudy() {
        Part part1 = buildPart(1L, "Part 1: Mô tả tranh", ESection.LISTENING);
        Part part2 = buildPart(2L, "Part 2: Hỏi - Đáp", ESection.LISTENING);

        Module modOpen = buildModule(10L, part1, 1, "Part 1 Trọng tâm", List.of("Nghe tranh người"));
        Module modLocked = buildModule(11L, part1, 2, "Part 1 Nâng cao", List.of("Nghe đồ vật"));
        Module modFree = buildModule(20L, part2, 3, "Part 2 Hỏi đáp", List.of("Câu hỏi Wh-"));

        Lesson l1 = buildLesson(101L, modOpen, 1, "Video 1", ELessonType.VIDEO, 300);
        Lesson l2 = buildLesson(102L, modLocked, 1, "Video 2", ELessonType.VIDEO, 360);
        Lesson l3 = buildLesson(201L, modFree, 1, "Video 3", ELessonType.VIDEO, 240);
        Lesson l4 = buildLesson(202L, modFree, 2, "Video 4", ELessonType.VIDEO, 240);

        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);
        LearningPathItem openItem = buildPathItem(1L, activePath, modOpen, 1, ELessonStatus.IN_PROGRESS, 50, "Điểm yếu Part 1");
        LearningPathItem lockedItem = buildPathItem(2L, activePath, modLocked, 2, ELessonStatus.LOCKED, 0, "Cần vượt ải trước");

        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1, part2));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(List.of(modOpen, modLocked, modFree));
        when(lessonRepository.findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(
                eq(List.of(10L, 11L, 20L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(List.of(l1, l2, l3, l4));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(PATH_ID))
                .thenReturn(List.of(openItem, lockedItem));
        when(lessonProgressRepository.countCompletedLessonsGroupedByModuleId(
                eq(USER_ID), eq(List.of(10L, 11L, 20L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(List.<Object[]>of(new Object[]{20L, 1L}));

        CourseCatalogResponse result = userCatalogService.getCourseCatalogForUser(USER_ID);

        assertThat(result.getHasLearningPath()).isTrue();
        assertThat(result.getTargetScore()).isEqualTo(650);
        assertThat(result.getRoadmapModuleCount()).isEqualTo(2);
        assertThat(result.getFreeStudyModuleCount()).isEqualTo(1);

        // Part 1 có 1 mô-đun đang mở và 1 mô-đun bị khóa (BR10)
        List<CatalogModuleItemResponse> part1Modules = result.getParts().get(0).getModules();
        assertThat(part1Modules).hasSize(2);
        assertThat(part1Modules.get(0).getAccessMode()).isEqualTo(ECatalogAccessMode.ROADMAP_OPEN);
        assertThat(part1Modules.get(0).getCanAccess()).isTrue();
        assertThat(part1Modules.get(0).getInRoadmap()).isTrue();
        assertThat(part1Modules.get(0).getRoadmapOrderNo()).isEqualTo(1);

        assertThat(part1Modules.get(1).getAccessMode()).isEqualTo(ECatalogAccessMode.ROADMAP_LOCKED);
        assertThat(part1Modules.get(1).getCanAccess()).isFalse();
        assertThat(part1Modules.get(1).getInRoadmap()).isTrue();
        assertThat(part1Modules.get(1).getLockReason()).isNotBlank();

        // Part 2 ngoài lộ trình -> mở FREE_STUDY và phản ánh tiến độ thật 1/2 bài (50%)
        CatalogModuleItemResponse part2Module = result.getParts().get(1).getModules().get(0);
        assertThat(part2Module.getAccessMode()).isEqualTo(ECatalogAccessMode.FREE_STUDY);
        assertThat(part2Module.getCanAccess()).isTrue();
        assertThat(part2Module.getInRoadmap()).isFalse();
        assertThat(part2Module.getCompletedLessons()).isEqualTo(1);
        assertThat(part2Module.getProgressPercent()).isEqualTo(50);

        // Xác nhận 0 N+1: mỗi repository chỉ được gọi đúng 1 lần duy nhất
        verify(partRepository, times(1)).findAllByOrderByIdAsc();
        verify(moduleRepository, times(1)).findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED);
        verify(lessonRepository, times(1)).findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(anyCollection(), eq(EModuleStatus.PUBLISHED));
    }

    @Test
    @DisplayName("UC17 - PASS 100% 7 Part (COMPLETED, 0 chặng): mở toàn bộ mô-đun ở chế độ FREE_STUDY")
    void getCourseCatalogForUser_whenPassedAllParts_opensAllModulesAsFreeStudy() {
        Part part1 = buildPart(1L, "Part 1: Mô tả tranh", ESection.LISTENING);
        Module mod1 = buildModule(10L, part1, 1, "Part 1 Cơ bản", List.of());
        LearningPath completedPath = buildLearningPath(PATH_ID, ELearningPathStatus.COMPLETED, 850);

        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(completedPath));
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(List.of(mod1));
        when(lessonRepository.findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(
                eq(List.of(10L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(Collections.emptyList());
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(PATH_ID))
                .thenReturn(Collections.emptyList());
        when(lessonProgressRepository.countCompletedLessonsGroupedByModuleId(
                eq(USER_ID), eq(List.of(10L)), eq(EModuleStatus.PUBLISHED)))
                .thenReturn(Collections.emptyList());

        CourseCatalogResponse result = userCatalogService.getCourseCatalogForUser(USER_ID);

        assertThat(result.getHasLearningPath()).isTrue();
        assertThat(result.getLearningPathStatus()).isEqualTo(ELearningPathStatus.COMPLETED);
        assertThat(result.getFreeStudyModuleCount()).isEqualTo(1);
        assertThat(result.getParts().get(0).getModules().get(0).getAccessMode())
                .isEqualTo(ECatalogAccessMode.FREE_STUDY);
        assertThat(result.getParts().get(0).getModules().get(0).getCanAccess()).isTrue();
    }

    @Test
    @DisplayName("UC17 - Biên không có mô-đun xuất bản: bỏ qua query IN rỗng và trả danh sách an toàn")
    void getCourseCatalogForUser_whenNoPublishedModules_skipsEmptyInQueriesSafely() {
        Part part1 = buildPart(1L, "Part 1: Mô tả tranh", ESection.LISTENING);
        when(partRepository.findAllByOrderByIdAsc()).thenReturn(List.of(part1));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(moduleRepository.findByStatusWithPartAndAbilitiesOrderBySequenceAsc(EModuleStatus.PUBLISHED))
                .thenReturn(Collections.emptyList());

        CourseCatalogResponse result = userCatalogService.getCourseCatalogForUser(USER_ID);

        assertThat(result.getTotalParts()).isEqualTo(1);
        assertThat(result.getTotalModules()).isZero();
        assertThat(result.getParts().get(0).getModules()).isEmpty();

        verify(lessonRepository, never()).findByModuleIdInAndStatusOrderByModuleIdAscOrderNoAsc(
                anyCollection(), eq(EModuleStatus.PUBLISHED));
    }

    private Part buildPart(Long id, String name, ESection section) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setDescription("Mô tả " + name);
        part.setSections(section);
        return part;
    }

    private Module buildModule(Long id, Part part, int sequence, String title, List<String> abilityNames) {
        Module module = new Module();
        module.setId(id);
        module.setPart(part);
        module.setSequence(sequence);
        module.setTitle(title);
        module.setDescriptions("Mục tiêu " + title);
        module.setStatus(EModuleStatus.PUBLISHED);
        List<ModuleAbility> maList = abilityNames.stream().map(name -> {
            Ability ability = new Ability();
            ability.setName(name);
            ModuleAbility ma = new ModuleAbility();
            ma.setModule(module);
            ma.setAbility(ability);
            return ma;
        }).toList();
        module.setModuleAbilities(maList);
        return module;
    }

    private Lesson buildLesson(Long id, Module module, int orderNo, String title, ELessonType type, int duration) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setModule(module);
        lesson.setOrderNo(orderNo);
        lesson.setTitle(title);
        lesson.setLessonType(type);
        lesson.setDurationSeconds(duration);
        lesson.setIsFree(true);
        lesson.setStatus(EModuleStatus.PUBLISHED);
        lesson.setVideoUrl("https://secret-cloudinary.example.com/private-video.mp4");
        return lesson;
    }

    private LearningPath buildLearningPath(Long id, ELearningPathStatus status, int targetScore) {
        TargetProfile profile = new TargetProfile();
        profile.setId(1L);
        profile.setTargetTotalScore(targetScore);
        profile.setDescription("Mục tiêu " + targetScore);

        LearningPath path = new LearningPath();
        path.setId(id);
        path.setStatus(status);
        path.setTargetProfile(profile);
        return path;
    }

    private LearningPathItem buildPathItem(
            Long id,
            LearningPath path,
            Module module,
            int orderNo,
            ELessonStatus status,
            int progress,
            String reason
    ) {
        LearningPathItem item = new LearningPathItem();
        item.setId(id);
        item.setLearningPath(path);
        item.setModule(module);
        item.setOrderNo(orderNo);
        item.setStatus(status);
        item.setProgressPercent(progress);
        item.setReason(reason);
        return item;
    }
}
