package com.example.be.service.content.implement;

import com.example.be.dto.response.content.RoadmapResponse;
import com.example.be.dto.response.content.UserModuleDetailResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ECatalogAccessMode;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserRoadmapMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Tests kiểm thử toàn diện nghiệp vụ UC09 (Lộ trình học cá nhân) và UC10 (Chi tiết Module):
 * - Luồng ngoại lệ chưa thi chẩn đoán (2a)
 * - Luồng thay thế PASS 100% 7 Part (4a)
 * - Gom nhóm đếm bài học chống N+1
 * - Chống truy cập mô-đun bị khóa BR10 (422 CONTENT_LOCKED) & không tồn tại (404)
 * - Đọc động cấu hình bài thi vượt ải theo AIM & V4 (passScore, requiredCompletionRate, testDurationMinutes, testSelectionMode)
 * - Lịch sử thi đỗ mô-đun trong quá khứ (UC10 Luồng 6b)
 */
@ExtendWith(MockitoExtension.class)
class UserRoadmapServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long PATH_ID = 100L;
    private static final Long MODULE_ID_1 = 1L;
    private static final Long MODULE_ID_2 = 2L;
    private static final Long TARGET_ID = 3L;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningPathItemRepository learningPathItemRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private ModuleTargetThresholdRepository moduleTargetThresholdRepository;

    @Mock
    private ModuleTestQuestionRepository moduleTestQuestionRepository;

    @Mock
    private ModuleTestAttemptRepository moduleTestAttemptRepository;

    private UserRoadmapServiceImpl userRoadmapService;

    @BeforeEach
    void setUp() {
        UserRoadmapMapper mapper = new UserRoadmapMapper();
        userRoadmapService = new UserRoadmapServiceImpl(
                learningPathRepository,
                learningPathItemRepository,
                lessonRepository,
                lessonProgressRepository,
                moduleRepository,
                moduleTargetThresholdRepository,
                moduleTestQuestionRepository,
                moduleTestAttemptRepository,
                mapper
        );
    }

    @Test
    @DisplayName("UC09 - Luồng 2a: Trả về null khi học viên chưa có lộ trình học hiện hành")
    void getCurrentUserRoadmap_whenNoPath_returnsNull() {
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(Collections.emptyList());

        RoadmapResponse result = userRoadmapService.getCurrentUserRoadmap(USER_ID);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("UC09 - Luồng 4a: Trả về trạng thái PASS 100% 7 Part khi lộ trình COMPLETED và không có chặng con")
    void getCurrentUserRoadmap_whenPassedAll7Parts_returnsCompletedEmptyPath() {
        LearningPath completedPath = buildLearningPath(PATH_ID, ELearningPathStatus.COMPLETED, 750);
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(completedPath));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(PATH_ID))
                .thenReturn(Collections.emptyList());

        RoadmapResponse result = userRoadmapService.getCurrentUserRoadmap(USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ELearningPathStatus.COMPLETED);
        assertThat(result.getIsPassedAllParts()).isTrue();
        assertThat(result.getProgressPercent()).isEqualTo(100);
        assertThat(result.getTotalModules()).isZero();
        assertThat(result.getModules()).isEmpty();
    }

    @Test
    @DisplayName("UC09 - Luồng chính: Nạp lộ trình ACTIVE, tính tiến độ và gom đếm bài học trong 1 query chống N+1")
    void getCurrentUserRoadmap_whenActivePathWithModules_calculatesProgressWithoutNPlus1() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);
        Module module1 = buildModule(MODULE_ID_1, "Nền tảng Part 1", 1, ESection.LISTENING, "Part 1");
        Module module2 = buildModule(MODULE_ID_2, "Ngữ pháp Part 5", 2, ESection.READING, "Part 5");

        LearningPathItem item1 = buildPathItem(11L, activePath, module1, 1, ELessonStatus.COMPLETED, 100);
        LearningPathItem item2 = buildPathItem(12L, activePath, module2, 2, ELessonStatus.IN_PROGRESS, 50);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(PATH_ID))
                .thenReturn(List.of(item1, item2));
        when(lessonRepository.countByModuleIdInAndStatus(List.of(MODULE_ID_1, MODULE_ID_2), EModuleStatus.PUBLISHED))
                .thenReturn(List.of(
                        new Object[]{MODULE_ID_1, 4L},
                        new Object[]{MODULE_ID_2, 6L}
                ));
        when(lessonProgressRepository.countCompletedLessonsGroupedByModuleId(
                USER_ID, List.of(MODULE_ID_1, MODULE_ID_2), EModuleStatus.PUBLISHED))
                .thenReturn(List.<Object[]>of(
                        new Object[]{MODULE_ID_2, 3L}
                ));

        RoadmapResponse result = userRoadmapService.getCurrentUserRoadmap(USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getIsPassedAllParts()).isFalse();
        assertThat(result.getTotalModules()).isEqualTo(2);
        assertThat(result.getCompletedModules()).isEqualTo(1);
        assertThat(result.getProgressPercent()).isEqualTo(50);
        assertThat(result.getModules()).hasSize(2);
        assertThat(result.getModules().get(0).getTotalLessons()).isEqualTo(4);
        assertThat(result.getModules().get(0).getCompletedLessons()).isEqualTo(4);
        assertThat(result.getModules().get(1).getTotalLessons()).isEqualTo(6);
        assertThat(result.getModules().get(1).getCompletedLessons()).isEqualTo(3);

        verify(lessonRepository).countByModuleIdInAndStatus(List.of(MODULE_ID_1, MODULE_ID_2), EModuleStatus.PUBLISHED);
    }

    @Test
    @DisplayName("UC10 - Luồng 2a (BR10): Ném 422 CONTENT_LOCKED khi học viên cố truy cập mô-đun đang bị khóa")
    void getModuleDetailForUser_whenModuleLocked_throwsContentLocked() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);
        Module lockedModule = buildModule(MODULE_ID_2, "Đọc hiểu Part 7", 2, ESection.READING, "Part 7");
        LearningPathItem lockedItem = buildPathItem(12L, activePath, lockedModule, 2, ELessonStatus.LOCKED, 0);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID_2))
                .thenReturn(Optional.of(lockedItem));

        assertThatThrownBy(() -> userRoadmapService.getModuleDetailForUser(USER_ID, MODULE_ID_2))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_LOCKED);
    }

    @Test
    @DisplayName("UC10 - Luồng 3a: Ném 404 RESOURCE_NOT_FOUND khi mô-đun không tồn tại hoặc chưa xuất bản")
    void getModuleDetailForUser_whenModuleNotInPathAndNotPublished_throwsResourceNotFound() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, 999L))
                .thenReturn(Optional.empty());
        when(moduleRepository.findByIdAndStatusWithPartAndAbilities(999L, EModuleStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoadmapService.getModuleDetailForUser(USER_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("UC17 - Học tự do: Cho phép mở chi tiết mô-đun PUBLISHED nằm ngoài lộ trình ở chế độ FREE_STUDY")
    void getModuleDetailForUser_whenModuleOutsideRoadmapAndPublished_returnsFreeStudyDetail() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);
        Module freeModule = buildModule(MODULE_ID_2, "Part 2 Ôn bổ trợ", 2, ESection.LISTENING, "Part 2");
        freeModule.setStatus(EModuleStatus.PUBLISHED);
        Lesson videoLesson = buildLesson(301L, "Chiến thuật câu hỏi đuôi", 1, ELessonType.VIDEO, 320);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID_2))
                .thenReturn(Optional.empty());
        when(moduleRepository.findByIdAndStatusWithPartAndAbilities(MODULE_ID_2, EModuleStatus.PUBLISHED))
                .thenReturn(Optional.of(freeModule));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID_2, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(videoLesson));
        when(lessonRepository.countPracticeQuestionsByLessonIdIn(List.of(301L)))
                .thenReturn(Collections.emptyList());
        when(lessonProgressRepository.findByUserIdAndLessonIdIn(USER_ID, List.of(301L)))
                .thenReturn(Collections.emptyList());

        UserModuleDetailResponse result = userRoadmapService.getModuleDetailForUser(USER_ID, MODULE_ID_2);

        assertThat(result.getModuleId()).isEqualTo(MODULE_ID_2);
        assertThat(result.getInRoadmap()).isFalse();
        assertThat(result.getAccessMode()).isEqualTo(ECatalogAccessMode.FREE_STUDY);
        assertThat(result.getStatus()).isEqualTo(ELessonStatus.UNLOCKED);
        assertThat(result.getModuleTest()).isNull();
        assertThat(result.getLessons()).hasSize(1);
        assertThat(result.getLessons().get(0).getIsLocked()).isFalse();
    }

    @Test
    @DisplayName("UC10 - BR03 & V4: Mở khóa bài thi vượt ải khi đạt tỷ lệ yêu cầu, đọc động ngưỡng AIM và số câu hỏi")
    void getModuleDetailForUser_whenProgressMeetsRequiredRate_unlocksModuleTestAndLoadsDynamicConfig() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 750);
        Module module = buildModule(MODULE_ID_1, "Nghe tranh Part 1", 1, ESection.LISTENING, "Part 1");
        module.setRequiredCompletionRate(80);
        module.setTestDurationMinutes(30);
        module.setTestQuestionCount(20);
        module.setTestSelectionMode(EModuleTestSelectionMode.MANUAL);

        LearningPathItem pathItem = buildPathItem(11L, activePath, module, 1, ELessonStatus.IN_PROGRESS, 85);

        Lesson videoLesson = buildLesson(201L, "Chiến thuật nghe mô tả người", 1, ELessonType.VIDEO, 620);
        Lesson practiceLesson = buildLesson(202L, "Luyện tập tranh văn phòng", 2, ELessonType.PRACTICE, 0);

        ModuleTargetThreshold customThreshold = new ModuleTargetThreshold();
        customThreshold.setPassScore(75);

        LessonProgress completedVideoProgress = new LessonProgress();
        completedVideoProgress.setLesson(videoLesson);
        completedVideoProgress.setIsCompleted(true);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID_1))
                .thenReturn(Optional.of(pathItem));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID_1, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(videoLesson, practiceLesson));
        when(lessonRepository.countPracticeQuestionsByLessonIdIn(List.of(201L, 202L)))
                .thenReturn(List.of(
                        new Object[]{201L, 3L},
                        new Object[]{202L, 10L}
                ));
        when(lessonProgressRepository.findByUserIdAndLessonIdIn(USER_ID, List.of(201L, 202L)))
                .thenReturn(List.of(completedVideoProgress));
        when(moduleTargetThresholdRepository.findByModuleIdAndTargetProfileId(MODULE_ID_1, TARGET_ID))
                .thenReturn(Optional.of(customThreshold));
        when(moduleTestQuestionRepository.countQuestionsGroupedByModuleIds(List.of(MODULE_ID_1)))
                .thenReturn(List.<Object[]>of(new Object[]{MODULE_ID_1, 18L}));
        when(moduleTestAttemptRepository.findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(USER_ID, MODULE_ID_1))
                .thenReturn(Collections.emptyList());

        UserModuleDetailResponse result = userRoadmapService.getModuleDetailForUser(USER_ID, MODULE_ID_1);

        assertThat(result.getModuleId()).isEqualTo(MODULE_ID_1);
        assertThat(result.getTotalLessons()).isEqualTo(2);
        assertThat(result.getTotalVideoLessons()).isEqualTo(1);
        assertThat(result.getCompletedVideoLessons()).isEqualTo(1);
        assertThat(result.getCompletedLessons()).isEqualTo(1);
        assertThat(result.getLessons().get(0).getIsCompleted()).isTrue();
        assertThat(result.getLessons().get(1).getIsCompleted()).isFalse();
        assertThat(result.getRequiredCompletionRate()).isEqualTo(80);
        assertThat(result.getLessons().get(0).getQuestionCount()).isEqualTo(3);
        assertThat(result.getLessons().get(1).getQuestionCount()).isEqualTo(10);

        assertThat(result.getModuleTest().getIsUnlocked()).isTrue();
        assertThat(result.getModuleTest().getPassScore()).isEqualTo(75);
        assertThat(result.getModuleTest().getDurationMinutes()).isEqualTo(30);
        assertThat(result.getModuleTest().getTotalQuestions()).isEqualTo(18);
        assertThat(result.getModuleTest().getHasPassed()).isFalse();
    }

    @Test
    @DisplayName("UC10 - Luồng 6b: Trả về điểm cao nhất và lượt thi gần nhất khi học viên đã từng thi đỗ mô-đun")
    void getModuleDetailForUser_whenPreviouslyPassed_returnsBestScoreAndLastAttemptId() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 650);
        Module module = buildModule(MODULE_ID_1, "Ngữ pháp Từ loại", 1, ESection.READING, "Part 5");
        module.setTestSelectionMode(EModuleTestSelectionMode.RANDOM_BY_ABILITY);
        module.setTestQuestionCount(25);
        module.setTestDurationMinutes(20);

        LearningPathItem completedItem = buildPathItem(11L, activePath, module, 1, ELessonStatus.COMPLETED, 100);

        ModuleTestAttempt latestAttempt = new ModuleTestAttempt();
        latestAttempt.setId(502L);
        latestAttempt.setScore(82);
        latestAttempt.setResult(ETestResult.PASS);

        ModuleTestAttempt earlierAttempt = new ModuleTestAttempt();
        earlierAttempt.setId(501L);
        earlierAttempt.setScore(90);
        earlierAttempt.setResult(ETestResult.PASS);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID_1))
                .thenReturn(Optional.of(completedItem));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID_1, EModuleStatus.PUBLISHED))
                .thenReturn(Collections.emptyList());
        when(moduleTargetThresholdRepository.findByModuleIdAndTargetProfileId(MODULE_ID_1, TARGET_ID))
                .thenReturn(Optional.empty());
        when(moduleTestAttemptRepository.findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(USER_ID, MODULE_ID_1))
                .thenReturn(List.of(latestAttempt, earlierAttempt));

        UserModuleDetailResponse result = userRoadmapService.getModuleDetailForUser(USER_ID, MODULE_ID_1);

        assertThat(result.getModuleTest().getHasPassed()).isTrue();
        assertThat(result.getModuleTest().getNeedsRetakeForNewAim()).isFalse();
        assertThat(result.getModuleTest().getBestScore()).isEqualTo(90);
        assertThat(result.getModuleTest().getLastAttemptId()).isEqualTo(502L);
        assertThat(result.getModuleTest().getIsUnlocked()).isTrue();
        assertThat(result.getModuleTest().getTotalQuestions()).isEqualTo(25);
        assertThat(result.getModuleTest().getDurationMinutes()).isEqualTo(20);
    }

    @Test
    @DisplayName("UC16 Bổ sung / BR06 (25/11): Nâng AIM bảo lưu 100% bài học nhưng chưa PASS ở AIM mới -> hasPassed=false & needsRetakeForNewAim=true")
    void getModuleDetailForUser_whenUpgradedAimRequiresRetake_setsHasPassedFalseAndNeedsRetakeTrue() {
        LearningPath activePath = buildLearningPath(PATH_ID, ELearningPathStatus.ACTIVE, 850);
        Module module = buildModule(MODULE_ID_1, "Nghe tranh cơ bản", 1, ESection.LISTENING, "Part 1");
        module.setTestSelectionMode(EModuleTestSelectionMode.RANDOM_BY_ABILITY);
        module.setTestQuestionCount(25);
        module.setTestDurationMinutes(25);

        LearningPathItem retakeItem = buildPathItem(12L, activePath, module, 1, ELessonStatus.IN_PROGRESS, 100);
        retakeItem.setIsCorrect(false);

        ModuleTestAttempt oldAimPassAttempt = new ModuleTestAttempt();
        oldAimPassAttempt.setId(601L);
        oldAimPassAttempt.setScore(68);
        oldAimPassAttempt.setResult(ETestResult.PASS);

        ModuleTargetThreshold threshold850 = new ModuleTargetThreshold();
        threshold850.setPassScore(80);

        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(activePath));
        when(learningPathItemRepository.findByLearningPathIdAndModuleIdWithDetails(PATH_ID, MODULE_ID_1))
                .thenReturn(Optional.of(retakeItem));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(MODULE_ID_1, EModuleStatus.PUBLISHED))
                .thenReturn(Collections.emptyList());
        when(moduleTargetThresholdRepository.findByModuleIdAndTargetProfileId(MODULE_ID_1, TARGET_ID))
                .thenReturn(Optional.of(threshold850));
        when(moduleTestAttemptRepository.findByUserIdAndModuleIdAndIsSubmittedTrueOrderBySubmittedAtDesc(USER_ID, MODULE_ID_1))
                .thenReturn(List.of(oldAimPassAttempt));

        UserModuleDetailResponse result = userRoadmapService.getModuleDetailForUser(USER_ID, MODULE_ID_1);

        assertThat(result.getModuleTest().getHasPassed()).isFalse();
        assertThat(result.getModuleTest().getNeedsRetakeForNewAim()).isTrue();
        assertThat(result.getModuleTest().getIsUnlocked()).isTrue();
        assertThat(result.getModuleTest().getBestScore()).isEqualTo(68);
        assertThat(result.getModuleTest().getPassScore()).isEqualTo(80);
        assertThat(result.getModuleTest().getDescription()).contains("80/100");
    }

    private LearningPath buildLearningPath(Long id, ELearningPathStatus status, int targetScore) {
        TargetProfile targetProfile = new TargetProfile();
        targetProfile.setId(TARGET_ID);
        targetProfile.setTargetTotalScore(targetScore);
        targetProfile.setDescription("Mục tiêu TOEIC " + targetScore);

        LearningPath path = new LearningPath();
        path.setId(id);
        path.setVersion(1);
        path.setStatus(status);
        path.setTargetProfile(targetProfile);
        return path;
    }

    private Module buildModule(Long id, String title, int sequence, ESection section, String partName) {
        Part part = new Part();
        part.setId(id + 50);
        part.setName(partName);
        part.setSections(section);

        Module module = new Module();
        module.setId(id);
        module.setTitle(title);
        module.setDescriptions("Mô tả mục tiêu " + title);
        module.setSequence(sequence);
        module.setDefaultPassScore(65);
        module.setRequiredCompletionRate(80);
        module.setPart(part);
        return module;
    }

    private LearningPathItem buildPathItem(
            Long id,
            LearningPath path,
            Module module,
            int orderNo,
            ELessonStatus status,
            int progressPercent
    ) {
        Ability ability = new Ability();
        ability.setId(id + 200);
        ability.setName("Kỹ năng " + module.getTitle());

        LearningPathItem item = new LearningPathItem();
        item.setId(id);
        item.setLearningPath(path);
        item.setModule(module);
        item.setAbility(ability);
        item.setOrderNo(orderNo);
        item.setStatus(status);
        item.setProgressPercent(progressPercent);
        item.setReason("Bổ trợ kỹ năng trọng tâm theo kết quả đầu vào");
        return item;
    }

    private Lesson buildLesson(Long id, String title, int orderNo, ELessonType type, int durationSeconds) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTitle(title);
        lesson.setOrderNo(orderNo);
        lesson.setLessonType(type);
        lesson.setDurationSeconds(durationSeconds);
        lesson.setStatus(EModuleStatus.PUBLISHED);
        lesson.setIsFree(true);
        return lesson;
    }
}
