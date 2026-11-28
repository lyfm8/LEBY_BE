package com.example.be.service.dashboard.implement;

import com.example.be.dto.response.dashboard.UserDashboardSummaryResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.content.EDashboardState;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.enums.user.ERole;
import com.example.be.mapper.dashboard.AdminDashboardMapper;
import com.example.be.mapper.dashboard.UserDashboardMapper;
import com.example.be.mapper.diagnostic.DiagnosticResultMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonProgressRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticAnswerRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.PartDiagnosticResultRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit Tests kiểm chứng đầy đủ 5 trạng thái vòng đời, công thức tính KPI/Streak/Biểu đồ tuần,
 * mô-đun bảo trì (Ngoại lệ 7a) và chống chia cho 0 trên {@link UserDashboardServiceImpl} (UC08).
 */
@ExtendWith(MockitoExtension.class)
class UserDashboardServiceImplTest {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private UserRepository userRepository;
    @Mock
    private LearningPathRepository learningPathRepository;
    @Mock
    private LearningPathItemRepository learningPathItemRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private ModuleTestAttemptRepository moduleTestAttemptRepository;
    @Mock
    private DiagnosticAttemptRepository diagnosticAttemptRepository;
    @Mock
    private PartDiagnosticResultRepository partDiagnosticResultRepository;
    @Mock
    private DiagnosticAnswerRepository diagnosticAnswerRepository;
    @Mock
    private DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;

    private UserDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        AdminDashboardMapper adminDashboardMapper = new AdminDashboardMapper();
        UserDashboardMapper userDashboardMapper = new UserDashboardMapper(adminDashboardMapper);
        DiagnosticResultMapper diagnosticResultMapper = new DiagnosticResultMapper();

        service = new UserDashboardServiceImpl(
                userRepository,
                learningPathRepository,
                learningPathItemRepository,
                lessonRepository,
                lessonProgressRepository,
                moduleTestAttemptRepository,
                diagnosticAttemptRepository,
                partDiagnosticResultRepository,
                diagnosticAnswerRepository,
                diagnosticTestQuestionRepository,
                diagnosticResultMapper,
                userDashboardMapper
        );
    }

    @Test
    void getStudentDashboardSummary_whenUserHasNoTarget_returnsNeedTargetState() {
        User user = createUser(1L, "Nguyễn Văn A", null);
        when(userRepository.findWithTargetProfileById(1L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(1L), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(1L))
                .thenReturn(Collections.emptyList());

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(1L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.NEED_TARGET);
        assertThat(result.getUser().getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(result.getUser().getTargetScore()).isNull();
        assertThat(result.getStats().getRoadmapProgressPercent()).isZero();
        assertThat(result.getWeeklyProgress()).hasSize(7);
        verifyNoInteractions(learningPathItemRepository, lessonProgressRepository);
    }

    @Test
    void getStudentDashboardSummary_whenUserHasTargetButNoDiagnostic_returnsNeedDiagnosticState() {
        TargetProfile target = createTarget(3L, 650, "Trung cấp");
        User user = createUser(2L, "Trần Thị B", target);

        DiagnosticAttempt inProgress = createDiagnosticAttempt(10L, user, EAttemptStatus.IN_PROGRESS, null, 0);

        when(userRepository.findWithTargetProfileById(2L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(2L), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(2L))
                .thenReturn(List.of(inProgress));

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(2L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.NEED_DIAGNOSTIC);
        assertThat(result.getUser().getTargetScore()).isEqualTo(650);
        assertThat(result.getDiagnostic().getHasCompletedDiagnostic()).isFalse();
        assertThat(result.getDiagnostic().getHasInProgressDiagnostic()).isTrue();
    }

    @Test
    void getStudentDashboardSummary_whenDiagnosticCompletedButNoPath_returnsSyncingRoadmapState() {
        TargetProfile target = createTarget(3L, 650, "Trung cấp");
        User user = createUser(3L, "Lê Văn C", target);
        DiagnosticAttempt submitted = createDiagnosticAttempt(
                20L, user, EAttemptStatus.SUBMITTED, LocalDateTime.now(VIETNAM_ZONE).minusMinutes(5), 1800
        );

        when(userRepository.findWithTargetProfileById(3L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(3L), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(3L))
                .thenReturn(List.of(submitted));
        when(partDiagnosticResultRepository.findByDiagnosticAttemptIdWithPart(20L))
                .thenReturn(Collections.emptyList());
        when(diagnosticAnswerRepository.findAnswerStatsByAttemptIds(List.of(20L)))
                .thenReturn(Collections.emptyList());
        when(diagnosticTestQuestionRepository.countQuestionsByTestIdsAndSection(List.of(100L)))
                .thenReturn(Collections.emptyList());

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(3L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.SYNCING_ROADMAP);
        assertThat(result.getDiagnostic().getHasCompletedDiagnostic()).isTrue();
        assertThat(result.getDiagnostic().getLatestAttemptId()).isEqualTo(20L);
    }

    @Test
    void getStudentDashboardSummary_whenActivePathWithProgress_computesStatsStreakWeeklyChartAndContinueLesson() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        TargetProfile target = createTarget(3L, 650, "Trung cấp");
        User user = createUser(4L, "Phạm Minh D", target);

        DiagnosticAttempt attempt = createDiagnosticAttempt(30L, user, EAttemptStatus.SUBMITTED, now.minusDays(2), 1800);
        LearningPath path = createLearningPath(40L, user, target, attempt, ELearningPathStatus.ACTIVE);

        Part part1 = createPart(1L, "Part 1", ESection.LISTENING);
        Part part5 = createPart(5L, "Part 5", ESection.READING);

        Module mod1 = createModule(101L, "Nghe tranh nền tảng", 1, EModuleStatus.PUBLISHED, part1);
        Module mod2 = createModule(102L, "Ngữ pháp từ loại Part 5", 2, EModuleStatus.PUBLISHED, part5);

        LearningPathItem item1 = createPathItem(201L, path, mod1, 1, ELessonStatus.COMPLETED, 100);
        LearningPathItem item2 = createPathItem(202L, path, mod2, 2, ELessonStatus.IN_PROGRESS, 50);

        Lesson lesson1 = createLesson(301L, mod2, "Danh từ và vị trí", 1, ELessonType.VIDEO, 600);
        Lesson lesson2 = createLesson(302L, mod2, "Động từ & thì cơ bản", 2, ELessonType.VIDEO, 900);
        Lesson lesson3 = createLesson(303L, mod2, "Luyện tập từ loại", 3, ELessonType.PRACTICE, 0);

        LessonProgress lpDone = createLessonProgress(401L, user, lesson1, 600, 100, true, now.minusDays(1));
        LessonProgress lpWatching = createLessonProgress(402L, user, lesson2, 300, 33, false, now);

        ModuleTestAttempt mta = new ModuleTestAttempt();
        mta.setId(501L);
        mta.setUser(user);
        mta.setModule(mod1);
        mta.setScore(80);
        mta.setResult(ETestResult.PASS);
        mta.setIsSubmitted(true);
        mta.setSubmittedAt(now.minusDays(1));

        PartDiagnosticResult pdr1 = createPartResult(part1, 85.0f, EPartDirective.PASS);
        PartDiagnosticResult pdr5 = createPartResult(part5, 45.0f, EPartDirective.ABILITY_DIAGNOSTIC);

        when(userRepository.findWithTargetProfileById(4L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(4L), anyCollection()))
                .thenReturn(List.of(path));
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(4L))
                .thenReturn(List.of(attempt));
        when(partDiagnosticResultRepository.findByDiagnosticAttemptIdWithPart(30L))
                .thenReturn(List.of(pdr5, pdr1));
        when(diagnosticAnswerRepository.findAnswerStatsByAttemptIds(List.of(30L)))
                .thenReturn(List.of(
                        new Object[]{30L, ESection.LISTENING, "Part 1", true},
                        new Object[]{30L, ESection.READING, "Part 5", true},
                        new Object[]{30L, ESection.READING, "Part 5", false}
                ));
        when(diagnosticTestQuestionRepository.countQuestionsByTestIdsAndSection(List.of(100L)))
                .thenReturn(List.of(
                        new Object[]{100L, ESection.LISTENING, "Part 1", 2L},
                        new Object[]{100L, ESection.READING, "Part 5", 2L}
                ));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(40L))
                .thenReturn(List.of(item1, item2));
        when(lessonProgressRepository.findAllByUserIdWithLessonAndModuleOrderByRecent(4L))
                .thenReturn(List.of(lpWatching, lpDone));
        when(moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(4L))
                .thenReturn(List.of(mta));
        when(lessonRepository.countByModuleIdInAndStatus(List.of(101L, 102L), EModuleStatus.PUBLISHED))
                .thenReturn(List.of(
                        new Object[]{101L, 2L},
                        new Object[]{102L, 3L}
                ));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(102L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(lessonRepository.countPracticeQuestionsByLessonIdIn(List.of(301L, 302L, 303L)))
                .thenReturn(Collections.singletonList(new Object[]{303L, 12L}));

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(4L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.IN_PROGRESS);
        assertThat(result.getStats().getRoadmapProgressPercent()).isEqualTo(50);
        assertThat(result.getStats().getCompletedModules()).isEqualTo(1);
        assertThat(result.getStats().getTotalModules()).isEqualTo(2);
        assertThat(result.getStats().getStreakDays()).isEqualTo(3);
        assertThat(result.getStats().getIsStudiedToday()).isTrue();
        // Tổng thời gian: 600s + 300s + 1800s = 2700s = 45 phút
        assertThat(result.getStats().getTotalStudyMinutes()).isEqualTo(45);
        // 2/4 câu đúng -> quy đổi thang 990 = 495 điểm
        assertThat(result.getStats().getPredictedScore()).isEqualTo(495);
        assertThat(result.getStats().getCanTakeFullMockTest()).isFalse();

        // Kiểm tra mô-đun đang học và bài học gợi ý tiếp tục (lesson2 đang xem dở 33%)
        assertThat(result.getCurrentModule()).isNotNull();
        assertThat(result.getCurrentModule().getModuleId()).isEqualTo(102L);
        assertThat(result.getCurrentModule().getIsUnderMaintenance()).isFalse();
        assertThat(result.getCurrentModule().getContinueLessonId()).isEqualTo(302L);
        assertThat(result.getCurrentModule().getUpcomingLessons()).hasSize(3);
        assertThat(result.getCurrentModule().getUpcomingLessons().get(2).getQuestionCount()).isEqualTo(12);

        // Đánh giá 7 Part đã sắp xếp theo partNo tăng dần và có nhãn Tiếng Việt
        assertThat(result.getPartEvaluations()).hasSize(2);
        assertThat(result.getPartEvaluations().get(0).getPartNo()).isEqualTo(1);
        assertThat(result.getPartEvaluations().get(0).getDirectiveLabel()).isEqualTo("Đạt chuẩn");
        assertThat(result.getPartEvaluations().get(1).getDirectiveLabel()).isEqualTo("Cần cải thiện");

        assertThat(result.getWeeklyProgress()).hasSize(7);
        assertThat(result.getRecentActivities()).hasSize(4);
        verify(lessonProgressRepository).findAllByUserIdWithLessonAndModuleOrderByRecent(4L);
    }

    @Test
    void getStudentDashboardSummary_whenCurrentModuleIsDraft_marksUnderMaintenance() {
        TargetProfile target = createTarget(3L, 650, "Trung cấp");
        User user = createUser(5L, "Hoàng E", target);
        LearningPath path = createLearningPath(41L, user, target, null, ELearningPathStatus.ACTIVE);

        Part part2 = createPart(2L, "Part 2", ESection.LISTENING);
        Module draftModule = createModule(105L, "Mô-đun đang bảo trì", 1, EModuleStatus.DRAFT, part2);
        LearningPathItem item = createPathItem(205L, path, draftModule, 1, ELessonStatus.IN_PROGRESS, 20);

        when(userRepository.findWithTargetProfileById(5L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(5L), anyCollection()))
                .thenReturn(List.of(path));
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(5L))
                .thenReturn(Collections.emptyList());
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(41L))
                .thenReturn(List.of(item));
        when(lessonProgressRepository.findAllByUserIdWithLessonAndModuleOrderByRecent(5L))
                .thenReturn(Collections.emptyList());
        when(moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(5L))
                .thenReturn(Collections.emptyList());
        when(lessonRepository.countByModuleIdInAndStatus(List.of(105L), EModuleStatus.PUBLISHED))
                .thenReturn(Collections.emptyList());

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(5L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.IN_PROGRESS);
        assertThat(result.getCurrentModule()).isNotNull();
        assertThat(result.getCurrentModule().getIsUnderMaintenance()).isTrue();
        assertThat(result.getCurrentModule().getContinueLessonId()).isNull();
        assertThat(result.getCurrentModule().getMaintenanceMessage()).contains("đang được cập nhật học liệu");
    }

    @Test
    void getStudentDashboardSummary_whenPassedAll7PartsCompletedPath_returnsCompletedWithoutDivisionByZero() {
        TargetProfile target = createTarget(4L, 750, "Trung cao cấp");
        User user = createUser(6L, "Đặng F", target);
        LearningPath completedEmptyPath = createLearningPath(42L, user, target, null, ELearningPathStatus.COMPLETED);

        when(userRepository.findWithTargetProfileById(6L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(6L), anyCollection()))
                .thenReturn(List.of(completedEmptyPath));
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(6L))
                .thenReturn(Collections.emptyList());
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(42L))
                .thenReturn(Collections.emptyList());
        when(lessonProgressRepository.findAllByUserIdWithLessonAndModuleOrderByRecent(6L))
                .thenReturn(Collections.emptyList());
        when(moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(6L))
                .thenReturn(Collections.emptyList());

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(6L);

        assertThat(result.getDashboardState()).isEqualTo(EDashboardState.COMPLETED);
        assertThat(result.getStats().getRoadmapProgressPercent()).isEqualTo(100);
        assertThat(result.getStats().getTotalModules()).isZero();
        assertThat(result.getStats().getIsPassedAllParts()).isTrue();
        assertThat(result.getStats().getCanTakeFullMockTest()).isTrue();
        assertThat(result.getCurrentModule()).isNull();
    }

    @Test
    void getStudentDashboardSummary_whenStreakEndsYesterdayVsBroken_calculatesStreakAccurately() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        TargetProfile target = createTarget(2L, 550, "Sơ trung cấp");
        User user = createUser(7L, "Bùi G", target);
        LearningPath path = createLearningPath(43L, user, target, null, ELearningPathStatus.ACTIVE);

        Part part1 = createPart(1L, "Part 1", ESection.LISTENING);
        Module mod = createModule(107L, "Mô tả tranh", 1, EModuleStatus.PUBLISHED, part1);
        LearningPathItem item = createPathItem(207L, path, mod, 1, ELessonStatus.IN_PROGRESS, 30);
        Lesson lesson1 = createLesson(307L, mod, "Bài 1", 1, ELessonType.VIDEO, 300);
        Lesson lesson2 = createLesson(308L, mod, "Bài 2", 2, ELessonType.VIDEO, 300);

        // Có học hôm qua và 2 ngày trước (hôm nay chưa học) -> giữ chuỗi 2 ngày với isStudiedToday = false
        LessonProgress yesterday = createLessonProgress(407L, user, lesson1, 200, 66, true, now.minusDays(1));
        LessonProgress twoDaysAgo = createLessonProgress(408L, user, lesson2, 150, 50, false, now.minusDays(2));

        when(userRepository.findWithTargetProfileById(7L)).thenReturn(Optional.of(user));
        when(learningPathRepository.findByUserIdAndStatusInOrderByVersionDescCreatedAtDesc(eq(7L), anyCollection()))
                .thenReturn(List.of(path));
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(7L))
                .thenReturn(Collections.emptyList());
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(43L))
                .thenReturn(List.of(item));
        when(lessonProgressRepository.findAllByUserIdWithLessonAndModuleOrderByRecent(7L))
                .thenReturn(List.of(yesterday, twoDaysAgo));
        when(moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(7L))
                .thenReturn(Collections.emptyList());
        when(lessonRepository.countByModuleIdInAndStatus(List.of(107L), EModuleStatus.PUBLISHED))
                .thenReturn(Collections.singletonList(new Object[]{107L, 2L}));
        when(lessonRepository.findByModuleIdAndStatusWithAbilityOrderByOrderNoAsc(107L, EModuleStatus.PUBLISHED))
                .thenReturn(List.of(lesson1, lesson2));
        when(lessonRepository.countPracticeQuestionsByLessonIdIn(List.of(307L, 308L)))
                .thenReturn(Collections.emptyList());

        UserDashboardSummaryResponse result = service.getStudentDashboardSummary(7L);

        assertThat(result.getStats().getStreakDays()).isEqualTo(2);
        assertThat(result.getStats().getIsStudiedToday()).isFalse();
        assertThat(result.getStats().getWeeklyActiveDays()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private User createUser(Long id, String fullName, TargetProfile targetProfile) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@gmail.com");
        user.setFullName(fullName);
        user.setRole(ERole.STUDENT);
        user.setTargetProfile(targetProfile);
        return user;
    }

    private TargetProfile createTarget(Long id, int score, String description) {
        TargetProfile tp = new TargetProfile();
        tp.setId(id);
        tp.setTargetTotalScore(score);
        tp.setDescription(description);
        return tp;
    }

    private DiagnosticAttempt createDiagnosticAttempt(
            Long id,
            User user,
            EAttemptStatus status,
            LocalDateTime completedAt,
            int durationSeconds
    ) {
        DiagnosticTest test = new DiagnosticTest();
        test.setId(100L);
        test.setTitle("Bài thi chẩn đoán TOEIC");
        test.setTestType(ETestType.DIAGNOSTIC);

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(id);
        attempt.setUser(user);
        attempt.setDiagnosticTest(test);
        attempt.setStatus(status);
        attempt.setStartedAt(completedAt != null ? completedAt.minusMinutes(30) : LocalDateTime.now(VIETNAM_ZONE));
        attempt.setCompletedAt(completedAt);
        attempt.setDurationSeconds(durationSeconds);
        return attempt;
    }

    private LearningPath createLearningPath(
            Long id,
            User user,
            TargetProfile target,
            DiagnosticAttempt attempt,
            ELearningPathStatus status
    ) {
        LearningPath lp = new LearningPath();
        lp.setId(id);
        lp.setUser(user);
        lp.setTargetProfile(target);
        lp.setDiagnosticAttempt(attempt);
        lp.setStatus(status);
        lp.setVersion(1);
        return lp;
    }

    private Part createPart(Long id, String name, ESection section) {
        Part part = new Part();
        part.setId(id);
        part.setName(name);
        part.setSections(section);
        return part;
    }

    private Module createModule(Long id, String title, int sequence, EModuleStatus status, Part part) {
        Module module = new Module();
        module.setId(id);
        module.setTitle(title);
        module.setSequence(sequence);
        module.setStatus(status);
        module.setPart(part);
        module.setRequiredCompletionRate(80);
        return module;
    }

    private LearningPathItem createPathItem(
            Long id,
            LearningPath path,
            Module module,
            int orderNo,
            ELessonStatus status,
            int progressPercent
    ) {
        Ability ability = new Ability();
        ability.setId(id);
        ability.setName("Kỹ năng trọng tâm " + orderNo);

        LearningPathItem item = new LearningPathItem();
        item.setId(id);
        item.setLearningPath(path);
        item.setModule(module);
        item.setAbility(ability);
        item.setOrderNo(orderNo);
        item.setStatus(status);
        item.setProgressPercent(progressPercent);
        item.setReason("Tập trung cải thiện kỹ năng theo kết quả chẩn đoán");
        return item;
    }

    private Lesson createLesson(Long id, Module module, String title, int orderNo, ELessonType type, int durationSec) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setModule(module);
        lesson.setTitle(title);
        lesson.setOrderNo(orderNo);
        lesson.setLessonType(type);
        lesson.setStatus(EModuleStatus.PUBLISHED);
        lesson.setDurationSeconds(durationSec);
        return lesson;
    }

    private LessonProgress createLessonProgress(
            Long id,
            User user,
            Lesson lesson,
            int watchedSeconds,
            int watchPercent,
            boolean isCompleted,
            LocalDateTime syncedAt
    ) {
        LessonProgress lp = new LessonProgress();
        lp.setId(id);
        lp.setUser(user);
        lp.setLesson(lesson);
        lp.setWatchedSeconds(watchedSeconds);
        lp.setWatchPercent(watchPercent);
        lp.setIsCompleted(isCompleted);
        lp.setLastSyncedAt(syncedAt);
        lp.setCompletedAt(isCompleted ? syncedAt : null);
        return lp;
    }

    private PartDiagnosticResult createPartResult(Part part, float score, EPartDirective directive) {
        PartDiagnosticResult pdr = new PartDiagnosticResult();
        pdr.setPart(part);
        pdr.setScore(score);
        pdr.setDirective(directive);
        return pdr;
    }
}
