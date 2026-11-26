package com.example.be.service.user;

import com.example.be.dto.request.user.UpdateUserRoleRequest;
import com.example.be.dto.response.user.AdminUserDetailResponse;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.enums.user.ERole;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.diagnostic.DiagnosticResultMapper;
import com.example.be.mapper.user.UserMapper;
import com.example.be.repository.content.LearningPathItemRepository;
import com.example.be.repository.content.LearningPathRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.ModuleTestAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticAnswerRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.setting.ModuleTargetThresholdRepository;
import com.example.be.repository.setting.ToeicScoreConversionRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.auth.service.MailService;
import com.example.be.service.user.implement.UserServiceImpl;
import com.example.be.service.user.util.UserExcelExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserServiceImpl} bao phủ toàn bộ quy tắc nghiệp vụ UC A02, BR11 và ngoại lệ 7d.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Pattern PASSWORD_POLICY_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$");

    @Mock
    private UserRepository userRepository;
    @Mock
    private DiagnosticAttemptRepository diagnosticAttemptRepository;
    @Mock
    private DiagnosticAnswerRepository diagnosticAnswerRepository;
    @Mock
    private DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;
    @Mock
    private ModuleTestAttemptRepository moduleTestAttemptRepository;
    @Mock
    private LearningPathRepository learningPathRepository;
    @Mock
    private LearningPathItemRepository learningPathItemRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private ModuleTargetThresholdRepository moduleTargetThresholdRepository;
    @Mock
    private ToeicScoreConversionRepository toeicScoreConversionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService mailService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        DiagnosticResultMapper diagnosticResultMapper = new DiagnosticResultMapper();
        UserExcelExporter userExcelExporter = new UserExcelExporter();

        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                diagnosticAttemptRepository,
                diagnosticAnswerRepository,
                diagnosticTestQuestionRepository,
                moduleTestAttemptRepository,
                learningPathRepository,
                learningPathItemRepository,
                lessonRepository,
                moduleTargetThresholdRepository,
                toeicScoreConversionRepository,
                diagnosticResultMapper,
                passwordEncoder,
                mailService,
                userExcelExporter
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_shouldIncludeAimTargetFromTargetProfile() {
        TargetProfile aim = new TargetProfile();
        aim.setId(3L);
        aim.setTargetTotalScore(650);

        User student = buildUser(10L, "student@gmail.com", "Nguyen Van A", ERole.STUDENT, true);
        student.setTargetProfile(aim);

        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findUsersByFilters("nguyen", ERole.STUDENT, true, 650, pageable))
                .thenReturn(new PageImpl<>(List.of(student), pageable, 1));

        Page<UserResponse> result = userService.getAllUsers(" nguyen ", "STUDENT", true, 650, pageable);

        assertThat(result.getContent()).hasSize(1);
        UserResponse dto = result.getContent().get(0);
        assertThat(dto.getAimScore()).isEqualTo(650);
        assertThat(dto.getAimTarget()).isEqualTo("AIM 650");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void toggleUserStatus_shouldThrowBr11_whenAdminTriesToLockSelf() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        when(userRepository.findWithTargetProfileById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.toggleUserStatus(1L, false))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("BR11");

        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserStatus_shouldThrowConflict7d_whenStudentInActiveExamAndNotForced() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(5L, "hv@gmail.com", "Hoc Vien", ERole.STUDENT, true);

        when(userRepository.findWithTargetProfileById(5L)).thenReturn(Optional.of(student));
        when(diagnosticAttemptRepository.existsByUserIdAndStatus(5L, EAttemptStatus.IN_PROGRESS)).thenReturn(true);
        when(moduleTestAttemptRepository.existsByUserIdAndIsSubmittedFalse(5L)).thenReturn(false);

        assertThatThrownBy(() -> userService.toggleUserStatus(5L, false))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_IN_ACTIVE_EXAM);

        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserStatus_shouldAbandonActiveDiagnosticsAndIncrementTokenVersion_whenForcedLock() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(5L, "hv@gmail.com", "Hoc Vien", ERole.STUDENT, true);
        student.setTokenVersion(2);

        DiagnosticAttempt inProgressAttempt = new DiagnosticAttempt();
        inProgressAttempt.setId(99L);
        inProgressAttempt.setStatus(EAttemptStatus.IN_PROGRESS);

        when(userRepository.findWithTargetProfileById(5L)).thenReturn(Optional.of(student));
        when(diagnosticAttemptRepository.findAllByUserIdAndStatus(5L, EAttemptStatus.IN_PROGRESS))
                .thenReturn(List.of(inProgressAttempt));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.toggleUserStatus(5L, true);

        assertThat(res.getIsActive()).isFalse();
        assertThat(res.getStatus()).isEqualTo("INACTIVE");
        assertThat(student.getTokenVersion()).isEqualTo(3);
        assertThat(inProgressAttempt.getStatus()).isEqualTo(EAttemptStatus.ABANDONED);
        verify(diagnosticAttemptRepository).saveAll(List.of(inProgressAttempt));
    }

    @Test
    void updateUserRole_shouldUpgradeStudentToTeacherAndInvalidateTokens() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(7L, "sv@gmail.com", "Hoc Vien", ERole.STUDENT, true);
        student.setTokenVersion(1);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(ERole.TEACHER);

        when(userRepository.findWithTargetProfileById(7L)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse updated = userService.updateUserRole(7L, request);

        assertThat(updated.getRole()).isEqualTo("TEACHER");
        assertThat(student.getTokenVersion()).isEqualTo(2);
    }

    @Test
    void updateUserRole_shouldRejectAdminRoleModification() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(7L, "sv@gmail.com", "Hoc Vien", ERole.STUDENT, true);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(ERole.ADMIN);

        when(userRepository.findWithTargetProfileById(7L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> userService.updateUserRole(7L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Học viên và Giáo viên");
    }

    @Test
    void resetUserPassword_shouldGenerateCompliantPasswordAndSendMail() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(8L, "reset@gmail.com", "Le Van B", ERole.STUDENT, true);
        student.setTokenVersion(0);

        when(userRepository.findById(8L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetUserPassword(8L);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendAdminResetPasswordEmail(eq("reset@gmail.com"), eq("Le Van B"), passwordCaptor.capture());

        String generated = passwordCaptor.getValue();
        assertThat(generated).matches(PASSWORD_POLICY_PATTERN);
        assertThat(student.getTokenVersion()).isEqualTo(1);
        assertThat(student.getPassword()).isEqualTo("$2a$10$encoded");
    }

    @Test
    void resetUserPassword_shouldPropagateMailFailureToTriggerRollback() {
        User admin = buildUser(1L, "admin@gmail.com", "Admin", ERole.ADMIN, true);
        setAuthenticatedUser(admin);
        User student = buildUser(8L, "reset@gmail.com", "Le Van B", ERole.STUDENT, true);

        when(userRepository.findById(8L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new BaseException(ErrorCode.MAIL_SEND_FAILED, "SMTP error"))
                .when(mailService).sendAdminResetPasswordEmail(any(), any(), any());

        assertThatThrownBy(() -> userService.resetUserPassword(8L))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MAIL_SEND_FAILED);
    }

    @Test
    void getUserDetail_shouldComputeMockTestScoreFromToeicConversionAndLoadLearningPath() {
        TargetProfile aim = new TargetProfile();
        aim.setId(2L);
        aim.setTargetTotalScore(650);

        User student = buildUser(12L, "detail@gmail.com", "Tran Thi C", ERole.STUDENT, true);
        student.setTargetProfile(aim);

        DiagnosticTest mockTest = new DiagnosticTest();
        mockTest.setId(20L);
        mockTest.setTitle("ETS Mock Test 2026");
        mockTest.setTestType(ETestType.FULL_MOCK_TEST);

        DiagnosticAttempt mockAttempt = new DiagnosticAttempt();
        mockAttempt.setId(200L);
        mockAttempt.setDiagnosticTest(mockTest);
        mockAttempt.setStatus(EAttemptStatus.SUBMITTED);
        mockAttempt.setCompletedAt(LocalDateTime.now());
        mockAttempt.setDurationSeconds(5400);

        ToeicScoreConversion conv80 = new ToeicScoreConversion();
        conv80.setCorrectCount(80);
        conv80.setListeningScore(410);
        conv80.setReadingScore(390);

        ToeicScoreConversion conv70 = new ToeicScoreConversion();
        conv70.setCorrectCount(70);
        conv70.setListeningScore(360);
        conv70.setReadingScore(340);

        Part part5 = new Part();
        part5.setId(5L);
        part5.setName("Part 5");
        part5.setSections(ESection.READING);

        Module module = new Module();
        module.setId(30L);
        module.setTitle("Thì động từ");
        module.setDefaultPassScore(65);
        module.setPart(part5);

        Ability ability = new Ability();
        ability.setId(40L);
        ability.setName("Verb Tense");

        LearningPath path = new LearningPath();
        path.setId(300L);
        path.setVersion(1);
        path.setStatus(ELearningPathStatus.ACTIVE);
        path.setTargetProfile(aim);

        LearningPathItem item = new LearningPathItem();
        item.setId(301L);
        item.setOrderNo(1);
        item.setModule(module);
        item.setAbility(ability);
        item.setProgressPercent(50);
        item.setStatus(ELessonStatus.IN_PROGRESS);
        item.setReason("Bổ trợ Verb Tense");

        ModuleTestAttempt mta = new ModuleTestAttempt();
        mta.setId(400L);
        mta.setModule(module);
        mta.setScore(80);
        mta.setResult(ETestResult.PASS);
        mta.setIsSubmitted(true);
        mta.setSubmittedAt(LocalDateTime.now());

        ModuleTargetThreshold threshold = new ModuleTargetThreshold();
        threshold.setModule(module);
        threshold.setPassScore(75);

        when(userRepository.findWithTargetProfileById(12L)).thenReturn(Optional.of(student));
        when(diagnosticAttemptRepository.existsByUserIdAndStatus(12L, EAttemptStatus.IN_PROGRESS)).thenReturn(false);
        when(moduleTestAttemptRepository.existsByUserIdAndIsSubmittedFalse(12L)).thenReturn(false);
        when(diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(12L)).thenReturn(List.of(mockAttempt));

        when(diagnosticTestQuestionRepository.countQuestionsByTestIdsAndSection(anyCollection()))
                .thenReturn(List.of(
                        new Object[]{20L, ESection.LISTENING, "Part 1", 100L},
                        new Object[]{20L, ESection.READING, "Part 5", 100L}
                ));
        List<Object[]> answerStatRows = new java.util.ArrayList<>();
        for (int i = 0; i < 80; i++) {
            answerStatRows.add(new Object[]{200L, ESection.LISTENING, "Part 1", true});
        }
        for (int i = 0; i < 70; i++) {
            answerStatRows.add(new Object[]{200L, ESection.READING, "Part 5", true});
        }
        when(diagnosticAnswerRepository.findAnswerStatsByAttemptIds(List.of(200L))).thenReturn(answerStatRows);
        when(toeicScoreConversionRepository.findAll()).thenReturn(List.of(conv80, conv70));

        when(learningPathRepository.findWithTargetProfileByUserIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(path));
        when(learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(300L)).thenReturn(List.of(item));
        when(lessonRepository.countByModuleIdIn(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{30L, 4L}));
        when(moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(12L)).thenReturn(List.of(mta));
        when(moduleTargetThresholdRepository.findByTargetProfileId(2L)).thenReturn(List.of(threshold));

        AdminUserDetailResponse detail = userService.getUserDetail(12L);

        assertThat(detail.getMockTestHistory()).hasSize(1);
        assertThat(detail.getMockTestHistory().get(0).getListeningScore()).isEqualTo(410);
        assertThat(detail.getMockTestHistory().get(0).getReadingScore()).isEqualTo(340);
        assertThat(detail.getMockTestHistory().get(0).getTotalScore()).isEqualTo(750);

        assertThat(detail.getLearningPath()).isNotNull();
        assertThat(detail.getLearningPath().getItems()).hasSize(1);
        assertThat(detail.getLearningPath().getItems().get(0).getCompletedLessons()).isEqualTo(2);
        assertThat(detail.getLearningPath().getItems().get(0).getTotalLessons()).isEqualTo(4);

        assertThat(detail.getModuleTestHistory()).hasSize(1);
        assertThat(detail.getModuleTestHistory().get(0).getPassScore()).isEqualTo(75);
    }

    @Test
    void exportUsersToExcel_shouldReturnValidXlsxBytes() {
        User student = buildUser(3L, "excel@gmail.com", "Excel User", ERole.STUDENT, true);
        when(userRepository.findAllUsersForExport(eq(null), eq(null), eq(null), eq(null), any(Sort.class)))
                .thenReturn(List.of(student));

        byte[] bytes = userService.exportUsersToExcel(null, null, null, null);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    private User buildUser(Long id, String email, String fullName, ERole role, boolean isActive) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(isActive);
        user.setTokenVersion(0);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private void setAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
