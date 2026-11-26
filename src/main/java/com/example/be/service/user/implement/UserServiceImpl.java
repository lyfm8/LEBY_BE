package com.example.be.service.user.implement;

import com.example.be.dto.request.user.UpdateUserRoleRequest;
import com.example.be.dto.response.user.AdminUserDetailResponse;
import com.example.be.dto.response.user.AdminUserDiagnosticHistoryItem;
import com.example.be.dto.response.user.AdminUserLearningPathItemDto;
import com.example.be.dto.response.user.AdminUserLearningPathResponse;
import com.example.be.dto.response.user.AdminUserMockTestHistoryItem;
import com.example.be.dto.response.user.AdminUserModuleTestHistoryItem;
import com.example.be.dto.response.user.UserActiveExamStatusResponse;
import com.example.be.dto.response.user.UserResponse;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.setting.ModuleTargetThreshold;
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELearningPathStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
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
import com.example.be.service.user.service.UserService;
import com.example.be.service.user.util.UserExcelExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý tài khoản người dùng dành cho Quản trị viên (UC A02).
 * Bao gồm tra cứu, xem chi tiết tiến trình học, khóa/mở khóa (BR11 + 7d),
 * nâng/hạ quyền (7c), đặt lại mật khẩu tự động gửi mail (7a) và xuất Excel (3a).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "Dữ liệu người dùng không tồn tại";
    private static final String BR11_SELF_LOCK_MESSAGE =
            "Quy tắc an toàn hệ thống (BR11): Quản trị viên không thể tự khóa tài khoản của chính mình!";
    private static final int GENERATED_PASSWORD_LENGTH = 10;
    private static final int STANDARD_TOEIC_MAX_SCORE = 990;
    private static final int STANDARD_TOEIC_SECTION_MAX_SCORE = 495;
    private static final int STANDARD_TOEIC_SECTION_QUESTIONS = 100;
    private static final int LISTENING_PART_MAX_NO = 4;
    private static final String UPPERCASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private static final String ALL_PASSWORD_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS;
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("Part\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final DiagnosticAnswerRepository diagnosticAnswerRepository;
    private final DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;
    private final ModuleTestAttemptRepository moduleTestAttemptRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final LessonRepository lessonRepository;
    private final ModuleTargetThresholdRepository moduleTargetThresholdRepository;
    private final ToeicScoreConversionRepository toeicScoreConversionRepository;
    private final DiagnosticResultMapper diagnosticResultMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UserExcelExporter userExcelExporter;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(
            String keyword,
            String role,
            Boolean isActive,
            Integer aimScore,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        ERole roleEnum = parseRoleFilter(role);

        Page<User> users = userRepository.findUsersByFilters(
                normalizedKeyword,
                roleEnum,
                isActive,
                aimScore,
                pageable
        );
        return users.map(userMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long id) {
        // Bước 1: Tìm người dùng kèm sẵn TargetProfile (ngoại lệ 8a nếu không tồn tại)
        User user = userRepository.findWithTargetProfileById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, USER_NOT_FOUND_MESSAGE));

        // Bước 2: Kiểm tra xem học viên có đang trong phòng thi trực tuyến nào không
        UserActiveExamStatusResponse activeExamStatus = checkActiveExamSession(id);

        // Bước 3: Nạp toàn bộ lượt làm bài Diagnostic + Full Mock Test và thống kê câu trả lời theo batch
        List<DiagnosticAttempt> allDiagnosticAttempts =
                diagnosticAttemptRepository.findAllByUserIdWithContextOrderByRecent(id);
        AttemptStatsContext statsContext = buildAttemptStatsContext(allDiagnosticAttempts);

        List<AdminUserDiagnosticHistoryItem> diagnosticHistory = buildDiagnosticHistory(
                allDiagnosticAttempts,
                statsContext
        );
        List<AdminUserMockTestHistoryItem> mockTestHistory = buildMockTestHistory(
                allDiagnosticAttempts,
                statsContext
        );

        // Bước 4: Nạp tiến độ lộ trình học tập hiện tại (ưu tiên ACTIVE, fallback COMPLETED / gần nhất)
        AdminUserLearningPathResponse learningPath = buildUserLearningPath(id);

        // Bước 5: Nạp lịch sử các lần thi vượt ải mô-đun kèm điểm chuẩn theo AIM của học viên
        List<AdminUserModuleTestHistoryItem> moduleTestHistory = buildModuleTestHistory(user);

        return userMapper.toDetailResponse(
                user,
                activeExamStatus,
                diagnosticHistory,
                learningPath,
                moduleTestHistory,
                mockTestHistory
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserActiveExamStatusResponse getActiveExamStatus(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, USER_NOT_FOUND_MESSAGE);
        }
        return checkActiveExamSession(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long id, boolean force) {
        // Bước 1: Tìm tài khoản cần khóa/mở khóa (kèm TargetProfile để trả DTO đầy đủ sau cập nhật)
        User user = userRepository.findWithTargetProfileById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, USER_NOT_FOUND_MESSAGE));

        boolean isCurrentlyActive = Boolean.TRUE.equals(user.getIsActive());

        // Bước 2: Kiểm tra quy tắc an toàn BR11 — Quản trị viên tuyệt đối không thể tự khóa chính mình
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && Objects.equals(currentUser.getId(), id) && isCurrentlyActive) {
            throw new BaseException(ErrorCode.BAD_REQUEST, BR11_SELF_LOCK_MESSAGE);
        }

        // Bước 3: Kiểm tra ngoại lệ 7d — nếu học viên đang thi trực tuyến và Admin chưa xác nhận ép khóa -> cảnh báo 409
        if (isCurrentlyActive && !force) {
            UserActiveExamStatusResponse activeExamStatus = checkActiveExamSession(id);
            if (activeExamStatus.isHasActiveExamSession()) {
                throw new BaseException(ErrorCode.USER_IN_ACTIVE_EXAM);
            }
        }

        // Bước 4: Cập nhật trạng thái mới
        boolean nextActiveState = !isCurrentlyActive;
        user.setIsActive(nextActiveState);

        // Bước 5: Nếu khóa tài khoản -> thu hồi toàn bộ JWT đang còn hạn và chuyển DiagnosticAttempt đang mở sang ABANDONED
        if (!nextActiveState) {
            user.setTokenVersion(nextTokenVersion(user));
            abandonInProgressDiagnostics(id);
        }

        User saved = userRepository.save(user);
        log.info("[ADMIN USER] Toggled status userId={}, isActive={}", id, nextActiveState);
        return userMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        // Bước 1: Guard không cho tự đổi vai trò chính mình để tránh mất quyền Admin
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && Objects.equals(currentUser.getId(), id)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Không thể tự thay đổi vai trò của tài khoản đang đăng nhập");
        }

        // Bước 2: Tìm người dùng cần cập nhật
        User user = userRepository.findWithTargetProfileById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, USER_NOT_FOUND_MESSAGE));

        ERole currentRole = user.getRole();
        ERole targetRole = request.getRole();

        // Bước 3: Chỉ cho phép nâng/hạ giữa STUDENT và TEACHER, không đụng vào tài khoản ADMIN
        if (currentRole == ERole.ADMIN || targetRole == ERole.ADMIN) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Chỉ hỗ trợ nâng/hạ quyền qua lại giữa Học viên và Giáo viên");
        }
        if (currentRole == targetRole) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Tài khoản hiện đã giữ vai trò này");
        }

        // Bước 4: Cập nhật role mới và tăng tokenVersion để vô hiệu hóa Access Token chứa role cũ
        user.setRole(targetRole);
        user.setTokenVersion(nextTokenVersion(user));

        User saved = userRepository.save(user);
        log.info("[ADMIN USER] Updated role userId={} from {} to {}", id, currentRole, targetRole);
        return userMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long id) {
        // Bước 1: Không cho phép tự đặt lại mật khẩu qua cổng Admin cho chính tài khoản đang đăng nhập
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser != null && Objects.equals(currentUser.getId(), id)) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Vui lòng sử dụng tính năng Đổi mật khẩu trong trang Hồ sơ cho tài khoản đang đăng nhập"
            );
        }

        // Bước 2: Tìm tài khoản mục tiêu trong DB
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, USER_NOT_FOUND_MESSAGE));

        // Bước 3: Sinh mật khẩu ngẫu nhiên đáp ứng quy chuẩn đăng ký và cập nhật vào DB (chưa commit)
        String rawNewPassword = generateSecureRandomPassword();
        user.setPassword(passwordEncoder.encode(rawNewPassword));
        user.setTokenVersion(nextTokenVersion(user));
        userRepository.saveAndFlush(user);

        // Bước 4: Gửi email thông báo mật khẩu mới; nếu gửi mail lỗi sẽ ném BaseException để rollback toàn bộ transaction
        mailService.sendAdminResetPasswordEmail(user.getEmail(), user.getFullName(), rawNewPassword);
        log.info("[ADMIN USER] Password reset and email sent for userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsersToExcel(String keyword, String role, Boolean isActive, Integer aimScore) {
        String normalizedKeyword = normalizeKeyword(keyword);
        ERole roleEnum = parseRoleFilter(role);

        List<User> users = userRepository.findAllUsersForExport(
                normalizedKeyword,
                roleEnum,
                isActive,
                aimScore,
                Sort.by("createdAt").descending()
        );
        return userExcelExporter.exportUsers(users);
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    private UserActiveExamStatusResponse checkActiveExamSession(Long userId) {
        boolean hasActiveDiagnostic = diagnosticAttemptRepository.existsByUserIdAndStatus(userId, EAttemptStatus.IN_PROGRESS);
        boolean hasActiveModuleTest = moduleTestAttemptRepository.existsByUserIdAndIsSubmittedFalse(userId);
        return UserActiveExamStatusResponse.builder()
                .hasActiveExamSession(hasActiveDiagnostic || hasActiveModuleTest)
                .hasActiveDiagnostic(hasActiveDiagnostic)
                .hasActiveModuleTest(hasActiveModuleTest)
                .build();
    }

    private void abandonInProgressDiagnostics(Long userId) {
        List<DiagnosticAttempt> activeAttempts =
                diagnosticAttemptRepository.findAllByUserIdAndStatus(userId, EAttemptStatus.IN_PROGRESS);
        if (activeAttempts.isEmpty()) {
            return;
        }
        // Chuyển toàn bộ phiên thi chẩn đoán / thi thử đang dang dở sang ABANDONED để đảm bảo toàn vẹn dữ liệu
        for (DiagnosticAttempt attempt : activeAttempts) {
            attempt.setStatus(EAttemptStatus.ABANDONED);
        }
        diagnosticAttemptRepository.saveAll(activeAttempts);
        log.info("[ADMIN USER] Abandoned {} in-progress diagnostic attempt(s) for locked userId={}",
                activeAttempts.size(), userId);
    }

    private AttemptStatsContext buildAttemptStatsContext(List<DiagnosticAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return AttemptStatsContext.empty();
        }

        List<Long> attemptIds = attempts.stream()
                .map(DiagnosticAttempt::getId)
                .toList();
        Set<Long> testIds = attempts.stream()
                .map(DiagnosticAttempt::getDiagnosticTest)
                .filter(Objects::nonNull)
                .map(test -> test.getId())
                .collect(Collectors.toSet());

        // Truy vấn batch tổng câu hỏi theo đề và theo Section (Listening/Reading)
        Map<Long, SectionCounts> testCountsByTestId = new HashMap<>();
        if (!testIds.isEmpty()) {
            List<Object[]> testRows = diagnosticTestQuestionRepository.countQuestionsByTestIdsAndSection(testIds);
            for (Object[] row : testRows) {
                Long testId = ((Number) row[0]).longValue();
                ESection section = (ESection) row[1];
                String partName = (String) row[2];
                int count = ((Number) row[3]).intValue();

                SectionCounts counts = testCountsByTestId.computeIfAbsent(testId, k -> new SectionCounts());
                counts.total += count;
                if (isListeningPart(section, partName)) {
                    counts.listening += count;
                } else {
                    counts.reading += count;
                }
            }
        }

        // Truy vấn batch toàn bộ đáp án của user để đếm số câu đúng tổng và theo Section
        Map<Long, SectionCounts> correctCountsByAttemptId = new HashMap<>();
        List<Object[]> answerRows = diagnosticAnswerRepository.findAnswerStatsByAttemptIds(attemptIds);
        for (Object[] row : answerRows) {
            Long attemptId = ((Number) row[0]).longValue();
            ESection section = (ESection) row[1];
            String partName = (String) row[2];
            boolean isCorrect = Boolean.TRUE.equals(row[3]);
            if (!isCorrect) {
                continue;
            }

            SectionCounts counts = correctCountsByAttemptId.computeIfAbsent(attemptId, k -> new SectionCounts());
            counts.total++;
            if (isListeningPart(section, partName)) {
                counts.listening++;
            } else {
                counts.reading++;
            }
        }

        return new AttemptStatsContext(testCountsByTestId, correctCountsByAttemptId);
    }

    private List<AdminUserDiagnosticHistoryItem> buildDiagnosticHistory(
            List<DiagnosticAttempt> allAttempts,
            AttemptStatsContext statsContext
    ) {
        List<AdminUserDiagnosticHistoryItem> result = new ArrayList<>();
        for (DiagnosticAttempt attempt : allAttempts) {
            if (attempt.getDiagnosticTest() == null
                    || attempt.getDiagnosticTest().getTestType() != ETestType.DIAGNOSTIC) {
                continue;
            }
            Long testId = attempt.getDiagnosticTest().getId();
            SectionCounts testTotals = statsContext.testCountsByTestId.getOrDefault(testId, new SectionCounts());
            SectionCounts correctCounts = statsContext.correctCountsByAttemptId.getOrDefault(
                    attempt.getId(),
                    new SectionCounts()
            );

            Integer totalQuestions = testTotals.total > 0 ? testTotals.total : null;
            Integer correctAnswers = attempt.getStatus() == EAttemptStatus.SUBMITTED ? correctCounts.total : null;
            Integer predictedScore = (attempt.getStatus() == EAttemptStatus.SUBMITTED && totalQuestions != null)
                    ? diagnosticResultMapper.calculateScaledToeicScore(
                    correctCounts.total,
                    totalQuestions,
                    STANDARD_TOEIC_MAX_SCORE
            )
                    : null;

            result.add(userMapper.toDiagnosticHistoryItem(attempt, correctAnswers, totalQuestions, predictedScore));
        }
        return result;
    }

    private List<AdminUserMockTestHistoryItem> buildMockTestHistory(
            List<DiagnosticAttempt> allAttempts,
            AttemptStatsContext statsContext
    ) {
        List<DiagnosticAttempt> submittedMockAttempts = allAttempts.stream()
                .filter(a -> a.getDiagnosticTest() != null
                        && a.getDiagnosticTest().getTestType() == ETestType.FULL_MOCK_TEST
                        && a.getStatus() == EAttemptStatus.SUBMITTED)
                .toList();

        if (submittedMockAttempts.isEmpty()) {
            return Collections.emptyList();
        }

        // Bảng quy đổi ETS chỉ có 101 bản ghi cố định (0..100), nạp 1 lần để tra cứu O(1)
        Map<Integer, ToeicScoreConversion> conversionMap = toeicScoreConversionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(ToeicScoreConversion::getCorrectCount, c -> c, (a, b) -> a));

        List<AdminUserMockTestHistoryItem> result = new ArrayList<>();
        for (DiagnosticAttempt attempt : submittedMockAttempts) {
            Long testId = attempt.getDiagnosticTest().getId();
            SectionCounts testTotals = statsContext.testCountsByTestId.getOrDefault(testId, new SectionCounts());
            SectionCounts correctCounts = statsContext.correctCountsByAttemptId.getOrDefault(
                    attempt.getId(),
                    new SectionCounts()
            );

            int listeningScore = resolveToeicSectionScore(
                    correctCounts.listening,
                    testTotals.listening,
                    true,
                    conversionMap
            );
            int readingScore = resolveToeicSectionScore(
                    correctCounts.reading,
                    testTotals.reading,
                    false,
                    conversionMap
            );

            result.add(AdminUserMockTestHistoryItem.builder()
                    .attemptId(attempt.getId())
                    .testTitle(attempt.getDiagnosticTest().getTitle())
                    .completedAt(attempt.getCompletedAt())
                    .durationSeconds(attempt.getDurationSeconds())
                    .listeningCorrect(correctCounts.listening)
                    .listeningTotal(testTotals.listening)
                    .listeningScore(listeningScore)
                    .readingCorrect(correctCounts.reading)
                    .readingTotal(testTotals.reading)
                    .readingScore(readingScore)
                    .totalScore(listeningScore + readingScore)
                    .build());
        }
        return result;
    }

    private int resolveToeicSectionScore(
            int correctCount,
            int totalQuestions,
            boolean isListening,
            Map<Integer, ToeicScoreConversion> conversionMap
    ) {
        if (totalQuestions <= 0) {
            return 0;
        }
        // Quy đổi số câu đúng về thang 100 nếu đề thi không đúng chuẩn 100 câu mỗi phần
        int lookupKey = (totalQuestions == STANDARD_TOEIC_SECTION_QUESTIONS)
                ? correctCount
                : Math.round((correctCount * (float) STANDARD_TOEIC_SECTION_QUESTIONS) / totalQuestions);
        lookupKey = Math.max(0, Math.min(STANDARD_TOEIC_SECTION_QUESTIONS, lookupKey));

        ToeicScoreConversion conversion = conversionMap.get(lookupKey);
        if (conversion != null) {
            return isListening ? conversion.getListeningScore() : conversion.getReadingScore();
        }
        // Fallback nếu bảng quy đổi chưa được seed
        return diagnosticResultMapper.calculateScaledToeicScore(
                correctCount,
                totalQuestions,
                STANDARD_TOEIC_SECTION_MAX_SCORE
        );
    }

    private AdminUserLearningPathResponse buildUserLearningPath(Long userId) {
        List<LearningPath> paths = learningPathRepository.findWithTargetProfileByUserIdOrderByCreatedAtDesc(userId);
        if (paths == null || paths.isEmpty()) {
            return null;
        }

        // Ưu tiên lộ trình đang học (ACTIVE), nếu đã hoàn thành chọn COMPLETED, sau cùng mới xét ARCHIVED gần nhất
        LearningPath currentPath = paths.stream()
                .filter(p -> p.getStatus() == ELearningPathStatus.ACTIVE)
                .findFirst()
                .or(() -> paths.stream().filter(p -> p.getStatus() == ELearningPathStatus.COMPLETED).findFirst())
                .orElse(paths.get(0));

        List<LearningPathItem> items =
                learningPathItemRepository.findByLearningPathIdWithDetailsOrderByOrderNoAsc(currentPath.getId());

        Map<Long, Integer> lessonCountByModuleId = loadLessonCountsForItems(items);
        List<AdminUserLearningPathItemDto> itemDtos = items.stream()
                .map(item -> {
                    Long moduleId = item.getModule() != null ? item.getModule().getId() : null;
                    int totalLessons = moduleId != null ? lessonCountByModuleId.getOrDefault(moduleId, 0) : 0;
                    return userMapper.toLearningPathItemDto(item, totalLessons);
                })
                .toList();

        return userMapper.toLearningPathResponse(currentPath, itemDtos);
    }

    private Map<Long, Integer> loadLessonCountsForItems(List<LearningPathItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> moduleIds = items.stream()
                .map(LearningPathItem::getModule)
                .filter(Objects::nonNull)
                .map(Module::getId)
                .collect(Collectors.toSet());
        if (moduleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> countMap = new HashMap<>();
        List<Object[]> rows = lessonRepository.countByModuleIdIn(moduleIds);
        for (Object[] row : rows) {
            Long moduleId = ((Number) row[0]).longValue();
            int count = ((Number) row[1]).intValue();
            countMap.put(moduleId, count);
        }
        return countMap;
    }

    private List<AdminUserModuleTestHistoryItem> buildModuleTestHistory(User user) {
        List<ModuleTestAttempt> attempts =
                moduleTestAttemptRepository.findAllByUserIdWithModuleOrderByRecent(user.getId());
        if (attempts.isEmpty()) {
            return Collections.emptyList();
        }

        // Nạp ngưỡng đỗ tùy chỉnh theo AIM hiện tại của học viên (1 query) thay vì hardcode 65 điểm
        Map<Long, Integer> passScoreByModuleId = new HashMap<>();
        if (user.getTargetProfile() != null) {
            List<ModuleTargetThreshold> thresholds =
                    moduleTargetThresholdRepository.findByTargetProfileId(user.getTargetProfile().getId());
            for (ModuleTargetThreshold threshold : thresholds) {
                if (threshold.getModule() != null && threshold.getPassScore() != null) {
                    passScoreByModuleId.put(threshold.getModule().getId(), threshold.getPassScore());
                }
            }
        }

        List<AdminUserModuleTestHistoryItem> result = new ArrayList<>();
        for (ModuleTestAttempt attempt : attempts) {
            Module module = attempt.getModule();
            Integer passScore = null;
            if (module != null) {
                passScore = passScoreByModuleId.getOrDefault(module.getId(), module.getDefaultPassScore());
            }
            result.add(userMapper.toModuleTestHistoryItem(attempt, passScore));
        }
        return result;
    }

    private boolean isListeningPart(ESection section, String partName) {
        if (section != null) {
            return section == ESection.LISTENING;
        }
        if (partName != null) {
            Matcher matcher = PART_NUMBER_PATTERN.matcher(partName);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1)) <= LISTENING_PART_MAX_NO;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private String generateSecureRandomPassword() {
        List<Character> chars = new ArrayList<>(GENERATED_PASSWORD_LENGTH);
        // Bảo đảm tối thiểu 1 chữ hoa, 1 chữ thường, 1 chữ số khớp regex của RegisterRequest
        chars.add(UPPERCASE_CHARS.charAt(SECURE_RANDOM.nextInt(UPPERCASE_CHARS.length())));
        chars.add(LOWERCASE_CHARS.charAt(SECURE_RANDOM.nextInt(LOWERCASE_CHARS.length())));
        chars.add(DIGIT_CHARS.charAt(SECURE_RANDOM.nextInt(DIGIT_CHARS.length())));

        for (int i = chars.size(); i < GENERATED_PASSWORD_LENGTH; i++) {
            chars.add(ALL_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_PASSWORD_CHARS.length())));
        }

        // Xáo trộn vị trí các ký tự bằng Fisher-Yates để tránh đoán thứ tự nhóm ký tự đầu
        for (int i = chars.size() - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            Character tmp = chars.get(i);
            chars.set(i, chars.get(j));
            chars.set(j, tmp);
        }

        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            return currentUser;
        }
        return null;
    }

    private int nextTokenVersion(User user) {
        return user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1;
    }

    private String normalizeKeyword(String keyword) {
        return (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    }

    private ERole parseRoleFilter(String role) {
        if (role == null || role.trim().isEmpty()) {
            return null;
        }
        try {
            return ERole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static class SectionCounts {
        int total = 0;
        int listening = 0;
        int reading = 0;
    }

    private static class AttemptStatsContext {
        final Map<Long, SectionCounts> testCountsByTestId;
        final Map<Long, SectionCounts> correctCountsByAttemptId;

        AttemptStatsContext(
                Map<Long, SectionCounts> testCountsByTestId,
                Map<Long, SectionCounts> correctCountsByAttemptId
        ) {
            this.testCountsByTestId = testCountsByTestId;
            this.correctCountsByAttemptId = correctCountsByAttemptId;
        }

        static AttemptStatsContext empty() {
            return new AttemptStatsContext(Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
