package com.example.be.service.diagnostic.implement;

import com.example.be.dto.request.diagnostic.StartDiagnosticRequest;
import com.example.be.dto.request.diagnostic.SubmitDiagnosticRequest;
import com.example.be.dto.response.diagnostic.DiagnosticResultResponse;
import com.example.be.dto.response.diagnostic.PartResultResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.user.TargetProfile;
import com.example.be.entity.user.User;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.repository.ability.UserAbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.repository.setting.TargetPartThresholdRepository;
import com.example.be.repository.user.UserRepository;
import com.example.be.service.content.AdaptivePathGeneratorService;
import com.example.be.service.diagnostic.service.DiagnosticScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai bộ máy chấm điểm 2 tầng cho bài kiểm tra chẩn đoán.
 *
 * <p>Luồng chính:
 * <pre>
 * User nộp bài
 *   → Tính điểm % từng Part (Tầng 1)
 *   → Phân nhánh PASS / CONFIRM / ABILITY_DIAGNOSTIC / FULL_PART theo TargetPartThreshold
 *   → Với CONFIRM & ABILITY_DIAGNOSTIC: tính accuracyRate từng Ability (Tầng 2)
 *   → Lưu UserAbility (WEAK / STABLE)
 *   → Gọi AdaptivePathGeneratorService → sinh LearningPath
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticScoringServiceImpl implements DiagnosticScoringService {

    // Số câu tối thiểu để kết quả Ability đủ độ tin cậy
    private static final int MIN_EVIDENCE_COUNT_FALLBACK = 1;
    // Ngưỡng cắt fallback khi DB chưa cấu hình AbilityEvaluationRule
    private static final float DEFAULT_CONFIRM_CUTOFF = 50.0f;
    private static final float DEFAULT_WEAK_CUTOFF = 75.0f;

    private final DiagnosticTestRepository diagnosticTestRepo;
    private final DiagnosticAttemptRepository diagnosticAttemptRepo;
    private final QuestionRepository questionRepo;
    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final UserAbilityRepository userAbilityRepo;
    private final UserRepository userRepo;
    private final PartRepository partRepo;
    private final AdaptivePathGeneratorService adaptivePathGeneratorService;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    @Override
    @Transactional
    public StartDiagnosticResponse startDiagnostic(Long userId, StartDiagnosticRequest request) {
        // Bước 1: Kiểm tra tồn tại học viên và đề thi trước khi mở phiên làm bài
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));

        DiagnosticTest test = diagnosticTestRepo.findById(request.getDiagnosticTestId())
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND));

        // QUY TẮC NGHIỆP VỤ: Hệ thống tách biệt đề DIAGNOSTIC (45p chẩn đoán đầu vào) và FULL_MOCK_TEST (120p thi thử đầu ra).
        // Chỉ đề loại DIAGNOSTIC mới được phép tạo lượt thi chẩn đoán sinh lộ trình.
        if (test.getTestType() != ETestType.DIAGNOSTIC) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Chỉ đề thi loại DIAGNOSTIC mới dùng được cho chẩn đoán năng lực");
        }

        // Bước 2: Khởi tạo bản ghi DiagnosticAttempt ở trạng thái IN_PROGRESS để làm mốc thời gian bắt đầu
        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setUser(user);
        attempt.setDiagnosticTest(test);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());
        attempt = diagnosticAttemptRepo.save(attempt);

        return StartDiagnosticResponse.builder()
                .attemptId(attempt.getId())
                .diagnosticTestId(test.getId())
                .startedAt(attempt.getStartedAt())
                .durationMinutes(test.getDurationMinutes())
                .build();
    }

    @Override
    @Transactional
    public DiagnosticResultResponse submitDiagnostic(Long userId, SubmitDiagnosticRequest request) {
        // Bước 1: Nạp lượt thi và kiểm tra bảo mật (chống IDOR + chống nộp lại nhiều lần)
        DiagnosticAttempt attempt = diagnosticAttemptRepo.findById(request.getAttemptId())
                .orElseThrow(() -> new BaseException(ErrorCode.DIAGNOSTIC_ATTEMPT_NOT_FOUND));

        // SECURITY: Đối chiếu userId từ JWT với chủ sở hữu attempt để ngăn học viên nộp bài hộ/phá bài người khác
        if (!attempt.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Bạn không có quyền nộp bài lượt thi này");
        }

        // Chỉ cho phép nộp bài khi lượt thi đang ở trạng thái IN_PROGRESS (tránh double-submit sinh trùng lộ trình)
        if (attempt.getStatus() != EAttemptStatus.IN_PROGRESS) {
            throw new BaseException(ErrorCode.DIAGNOSTIC_ALREADY_SUBMITTED);
        }

        // Bước 2: Tải trước toàn bộ câu hỏi của đề kèm Part & Ability (1 query JOIN FETCH) rồi chấm đúng/sai từng đáp án
        DiagnosticTest test = attempt.getDiagnosticTest();
        Map<Long, Question> questionMap = loadQuestionMapWithAbilities(test);
        List<DiagnosticAnswer> answers = buildAndSaveAnswers(attempt, request.getAnswers(), questionMap);
        attempt.getAnswers().addAll(answers);

        // Bước 3: Chốt trạng thái bài thi đã nộp và ghi nhận thời lượng làm bài thực tế
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.now());
        if (request.getDurationSeconds() != null) {
            attempt.setDurationSeconds(request.getDurationSeconds());
        }

        // Bước 4: Chấm điểm Tầng 1 (Macro) — tính % điểm đúng từng Part trên tổng số câu của đề,
        // đối chiếu TargetPartThreshold (3 mốc → 4 nhánh: PASS / CONFIRM / ABILITY_DIAGNOSTIC / FULL_PART)
        List<PartDiagnosticResult> partResults = scoreAllParts(attempt, request.getTargetProfileId(), answers, questionMap);
        attempt.getPartResults().addAll(partResults);
        diagnosticAttemptRepo.save(attempt);

        // Bước 5: Chấm điểm Tầng 2 (Micro) — chỉ soi kỹ năng con (Ability) cho các Part rơi vào CONFIRM hoặc ABILITY_DIAGNOSTIC,
        // áp dụng đánh giá nhị phân 2 mốc cắt (developingThreshold 50% vs stableThreshold 75%) và lưu UserAbility
        evaluateAndSaveUserAbilities(attempt, partResults, request.getTargetProfileId(), answers, questionMap);

        // Bước 6: Gọi thuật toán sinh lộ trình thích ứng ngay trong cùng Transaction
        // (đảm bảo nếu sinh lộ trình lỗi thì toàn bộ quá trình nộp bài được rollback đồng nhất)
        LearningPath learningPath = adaptivePathGeneratorService.generateAdaptivePath(attempt, request.getTargetProfileId());
        syncUserTargetProfile(attempt.getUser(), learningPath.getTargetProfile());
        log.info("Sinh lộ trình thành công cho user={}, learningPathId={}", userId, learningPath.getId());

        // Bước 7: Tổng hợp kết quả trả về cho trang Báo cáo chẩn đoán (totalQuestions lấy theo tổng câu của đề, không lấy theo answers.size)
        int totalCorrect = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        List<PartResultResponse> partResultResponses = partResults.stream()
                .map(pr -> PartResultResponse.builder()
                        .partId(pr.getPart().getId())
                        .partName(pr.getPart().getName())
                        .score(pr.getScore())
                        .directive(pr.getDirective().name())
                        .build())
                .collect(Collectors.toList());

        return DiagnosticResultResponse.builder()
                .attemptId(attempt.getId())
                .totalCorrect(totalCorrect)
                .totalQuestions(questionMap.size())
                .partResults(partResultResponses)
                .learningPathId(learningPath.getId())
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Tải toàn bộ câu hỏi thuộc đề thi kèm sẵn quan hệ Part và QuestionAbilities.
     * Dùng LEFT JOIN FETCH trong 1 truy vấn duy nhất để triệt tiêu lỗi N+1 Query khi chấm Tầng 1 & Tầng 2.
     */
    private Map<Long, Question> loadQuestionMapWithAbilities(DiagnosticTest test) {
        List<Long> questionIds = test.getTestQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toList());

        // Chuyển sang Map<questionId, Question> để tra cứu O(1) thay vì duyệt vòng lặp lồng nhau O(N*M)
        return questionRepo.findAllWithAbilitiesByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
    }

    /**
     * Chấm đúng/sai từng câu trả lời và tạo danh sách Entity DiagnosticAnswer để lưu DB.
     * SECURITY: Khử trùng lặp questionId nhằm ngăn client gửi lặp cùng 1 câu đúng nhiều lần để cộng dồn điểm.
     */
    private List<DiagnosticAnswer> buildAndSaveAnswers(
            DiagnosticAttempt attempt,
            List<SubmitDiagnosticRequest.AnswerItem> answerItems,
            Map<Long, Question> questionMap) {

        List<DiagnosticAnswer> result = new ArrayList<>();
        if (answerItems == null || answerItems.isEmpty()) {
            return result;
        }

        // Set ghi nhận các câu hỏi đã chấm trong lần nộp này — đảm bảo mỗi câu chỉ được tính tối đa 1 lần
        Set<Long> seenQuestionIds = new HashSet<>();
        for (SubmitDiagnosticRequest.AnswerItem item : answerItems) {
            if (item == null || item.getQuestionId() == null) {
                continue;
            }
            if (!seenQuestionIds.add(item.getQuestionId())) {
                log.warn("Bỏ qua questionId={} bị gửi trùng trong attemptId={}", item.getQuestionId(), attempt.getId());
                continue;
            }

            // Kiểm tra câu hỏi có thực sự nằm trong đề thi hiện tại không (chống chèn câu lạ từ bên ngoài đề)
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                log.warn("Bỏ qua questionId={} không thuộc đề thi attemptId={}", item.getQuestionId(), attempt.getId());
                continue;
            }

            boolean isCorrect = isAnswerCorrect(question.getCorrectAnswer(), item.getUserAnswer());

            DiagnosticAnswer answer = new DiagnosticAnswer();
            answer.setDiagnosticAttempt(attempt);
            answer.setQuestion(question);
            answer.setUserAnswer(item.getUserAnswer());
            answer.setIsCorrect(isCorrect);
            answer.setIsFlagged(false);
            result.add(answer);
        }

        return result;
    }

    /**
     * So sánh đáp án học viên chọn với đáp án đúng lưu trong DB.
     * Cả hai đều có định dạng JSON chuẩn: {"selected": "A"}.
     * Loại bỏ mọi khoảng trắng và so sánh không phân biệt hoa/thường để tránh lệch do format JSON.
     */
    private boolean isAnswerCorrect(String correctAnswerJson, String userAnswerJson) {
        if (correctAnswerJson == null || userAnswerJson == null) {
            return false;
        }
        return correctAnswerJson.replaceAll("\\s", "")
                .equalsIgnoreCase(userAnswerJson.replaceAll("\\s", ""));
    }

    /**
     * Chấm điểm Tầng 1 (Macro - cấp Part):
     * Tính % câu đúng của từng Part trên TỔNG SỐ CÂU CỦA ĐỀ (kể cả câu/Part học viên bỏ trống),
     * sau đó tra TargetPartThreshold của mục tiêu AIM để phân 4 nhánh:
     * - score >= passScore (VD 80%)           → PASS (miễn học cả Part)
     * - confirmingScore <= score < passScore  → CONFIRM (cận chuẩn, xuống Tầng 2 lọc mốc nhẹ)
     * - fullPartThreshold <= score < confirm  → ABILITY_DIAGNOSTIC (Part yếu, xuống Tầng 2 lọc mốc chặt)
     * - score < fullPartThreshold (VD 35%)    → FULL_PART (mất gốc, học toàn bộ Part)
     */
    private List<PartDiagnosticResult> scoreAllParts(
            DiagnosticAttempt attempt,
            Long targetProfileId,
            List<DiagnosticAnswer> answers,
            Map<Long, Question> questionMap) {

        // Đưa danh sách đáp án về Map<questionId, DiagnosticAnswer> để tra cứu O(1); mergeFunction (first, second) -> first phòng trùng khóa
        Map<Long, DiagnosticAnswer> answerByQuestionId = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (first, second) -> first));

        // QUAN TRỌNG: Nhóm theo TOÀN BỘ câu hỏi của đề (questionMap.values()) thay vì theo answers học viên gửi lên.
        // Cách này đảm bảo: (1) bỏ trống câu không làm đội điểm %, (2) bỏ trắng cả 1 Part vẫn sinh kết quả 0% (FULL_PART).
        Map<Long, List<Question>> questionsByPart = new HashMap<>();
        for (Question q : questionMap.values()) {
            if (q != null && q.getPart() != null) {
                questionsByPart.computeIfAbsent(q.getPart().getId(), k -> new ArrayList<>()).add(q);
            }
        }

        // Tải toàn bộ 7 ngưỡng Part của mục tiêu AIM trong đúng 1 truy vấn để tránh N+1 Query trong vòng lặp bên dưới
        Map<Long, TargetPartThreshold> thresholdByPartId = targetPartThresholdRepo
                .findByTargetProfileId(targetProfileId)
                .stream()
                .collect(Collectors.toMap(t -> t.getPart().getId(), t -> t));

        List<PartDiagnosticResult> results = new ArrayList<>();

        for (Map.Entry<Long, List<Question>> entry : questionsByPart.entrySet()) {
            Long partId = entry.getKey();
            List<Question> partQuestions = entry.getValue();

            // Mẫu số là tổng câu của Part trong đề; câu nào học viên bỏ trống (ans == null) mặc định tính sai
            int total = partQuestions.size();
            int correct = (int) partQuestions.stream()
                    .filter(q -> {
                        DiagnosticAnswer ans = answerByQuestionId.get(q.getId());
                        return ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
                    })
                    .count();
            float score = total > 0 ? (correct * 100.0f) / total : 0.0f;

            // Tra ngưỡng động từ DB; nếu Admin chưa cấu hình ngưỡng cho Part này thì fallback về ABILITY_DIAGNOSTIC để không bỏ sót lộ trình
            TargetPartThreshold threshold = thresholdByPartId.get(partId);
            if (threshold == null) {
                log.warn("Chưa cấu hình TargetPartThreshold cho partId={}, targetProfileId={}", partId, targetProfileId);
            }

            EPartDirective directive = threshold != null
                    ? threshold.determineDirective(score)
                    : EPartDirective.ABILITY_DIAGNOSTIC;

            PartDiagnosticResult partResult = new PartDiagnosticResult();
            partResult.setDiagnosticAttempt(attempt);
            // Dùng getReferenceById để lấy JPA proxy gán khóa ngoại mà không tốn thêm 1 câu lệnh SELECT vào bảng parts
            partResult.setPart(partRepo.getReferenceById(partId));
            partResult.setScore(score);
            partResult.setDirective(directive);
            results.add(partResult);
        }

        return results;
    }

    /**
     * Chấm điểm Tầng 2 (Micro - cấp Ability):
     * Chỉ áp dụng cho các Part có directive là CONFIRM hoặc ABILITY_DIAGNOSTIC.
     * Đánh giá NHỊ PHÂN (WEAK vs STABLE) dựa trên 2 mốc cắt của AbilityEvaluationRule:
     * - Nhánh CONFIRM            → so với developingThreshold (mẫu 50%, lọc thoáng vì Part đã gần đạt)
     * - Nhánh ABILITY_DIAGNOSTIC → so với stableThreshold (mẫu 75%, siết chặt vì Part đang yếu)
     * - Nếu số câu hỏi của Ability < minEvidenceCount → gắn UNCERTAIN (chưa đủ bằng chứng kết luận).
     */
    private void evaluateAndSaveUserAbilities(
            DiagnosticAttempt attempt,
            List<PartDiagnosticResult> partResults,
            Long targetProfileId,
            List<DiagnosticAnswer> answers,
            Map<Long, Question> questionMap) {

        // Tải toàn bộ AbilityEvaluationRule trong 1 query để tra cứu O(1), tránh query lẻ trong vòng lặp
        Map<Long, AbilityEvaluationRule> ruleByAbilityId = abilityRuleRepo.findAllWithDetails()
                .stream()
                .filter(r -> r.getAbility() != null)
                .collect(Collectors.toMap(r -> r.getAbility().getId(), r -> r));

        // Xóa snapshot UserAbility cũ của học viên trước khi ghi kết quả chẩn đoán mới nhất
        List<UserAbility> existingAbilities = userAbilityRepo.findByUserIdWithDetails(attempt.getUser().getId());
        if (!existingAbilities.isEmpty()) {
            userAbilityRepo.deleteAll(existingAbilities);
        }

        Map<Long, DiagnosticAnswer> answerByQuestionId = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (first, second) -> first));

        List<UserAbility> userAbilitiesToSave = new ArrayList<>();

        for (PartDiagnosticResult partResult : partResults) {
            EPartDirective directive = partResult.getDirective();
            // Bỏ qua Tầng 2 đối với PASS (đã vững cả Part) và FULL_PART (học toàn bộ Part, không cần lọc Ability)
            if (directive == EPartDirective.PASS || directive == EPartDirective.FULL_PART) {
                continue;
            }

            Long partId = partResult.getPart().getId();
            Map<Long, AbilityStat> statMap = calculateAbilityStats(partId, answerByQuestionId, questionMap);

            for (Map.Entry<Long, AbilityStat> entry : statMap.entrySet()) {
                Long abilityId = entry.getKey();
                AbilityStat stat = entry.getValue();
                float accuracyRate = stat.totalQuestions > 0
                        ? (stat.correctQuestions * 100.0f) / stat.totalQuestions
                        : 0.0f;

                AbilityEvaluationRule rule = ruleByAbilityId.get(abilityId);
                EAbilityStatus status;
                if (rule != null) {
                    int minEvidence = rule.getMinEvidenceCount() != null ? rule.getMinEvidenceCount() : MIN_EVIDENCE_COUNT_FALLBACK;
                    if (stat.totalQuestions < minEvidence) {
                        // Số câu hỏi xuất hiện trong đề quá ít, chưa đủ độ tin cậy thống kê để kết luận WEAK hay STABLE
                        status = EAbilityStatus.UNCERTAIN;
                    } else {
                        // Đánh giá nhị phân: rule.isWeak tự chọn mốc developingThreshold (CONFIRM) hoặc stableThreshold (ABILITY_DIAGNOSTIC)
                        status = rule.isWeak(accuracyRate, directive) ? EAbilityStatus.WEAK : EAbilityStatus.STABLE;
                    }
                } else {
                    // Fallback an toàn khi Admin chưa cấu hình rule cho Ability này trong DB
                    log.warn("Chưa cấu hình AbilityEvaluationRule cho abilityId={}, áp dụng ngưỡng fallback", abilityId);
                    float cutoff = (directive == EPartDirective.CONFIRM) ? DEFAULT_CONFIRM_CUTOFF : DEFAULT_WEAK_CUTOFF;
                    status = accuracyRate < cutoff ? EAbilityStatus.WEAK : EAbilityStatus.STABLE;
                }

                UserAbility userAbility = new UserAbility();
                userAbility.setUser(attempt.getUser());
                userAbility.setAbility(buildAbilityRef(abilityId, stat.abilityObj));
                userAbility.setPart(partRepo.getReferenceById(partId));
                userAbility.setStatus(status);
                userAbility.setAccuracyRate(accuracyRate);
                userAbility.setEvidenceCount(stat.totalQuestions);
                userAbilitiesToSave.add(userAbility);
            }
        }

        // Lưu hàng loạt 1 lần (Batch Save) thay vì gọi save() lẻ trong vòng lặp để tối ưu hiệu năng ghi DB
        if (!userAbilitiesToSave.isEmpty()) {
            userAbilityRepo.saveAll(userAbilitiesToSave);
        }
    }

    /**
     * Tính thống kê đúng/sai cho từng Ability trong một Part dựa trên TOÀN BỘ câu hỏi của đề.
     * Lưu ý: 1 câu hỏi có thể gắn nhiều Ability (quan hệ N-N qua QuestionAbility), nên kết quả câu đó
     * được cộng vào tất cả Ability liên quan; câu học viên bỏ trống vẫn tính vào totalQuestions (và tính sai).
     */
    private Map<Long, AbilityStat> calculateAbilityStats(
            Long partId,
            Map<Long, DiagnosticAnswer> answerByQuestionId,
            Map<Long, Question> questionMap) {

        Map<Long, AbilityStat> statMap = new HashMap<>();
        for (Question question : questionMap.values()) {
            if (question == null || question.getPart() == null) continue;
            if (!question.getPart().getId().equals(partId)) continue;

            DiagnosticAnswer answer = answerByQuestionId.get(question.getId());
            boolean correct = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());

            if (question.getQuestionAbilities() != null) {
                for (QuestionAbility qa : question.getQuestionAbilities()) {
                    Ability ability = qa.getAbility();
                    if (ability == null) continue;
                    AbilityStat stat = statMap.computeIfAbsent(ability.getId(),
                            k -> new AbilityStat(ability.getId(), ability));
                    stat.totalQuestions++;
                    if (correct) stat.correctQuestions++;
                }
            }
        }
        return statMap;
    }

    /**
     * Đồng bộ cập nhật mục tiêu AIM của học viên nếu chưa có hoặc có thay đổi khi nộp bài chẩn đoán.
     */
    private void syncUserTargetProfile(User user, TargetProfile targetProfile) {
        if (user == null || targetProfile == null) {
            return;
        }
        if (user.getTargetProfile() == null
                || !targetProfile.getId().equals(user.getTargetProfile().getId())) {
            user.setTargetProfile(targetProfile);
            userRepo.save(user);
        }
    }

    /**
     * Xây dựng reference Ability để gán vào UserAbility — tránh tạo thêm proxy thừa.
     */
    private Ability buildAbilityRef(Long abilityId, Ability loaded) {
        if (loaded != null) return loaded;
        Ability ref = new Ability();
        ref.setId(abilityId);
        return ref;
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    private static class AbilityStat {
        final Long abilityId;
        final Ability abilityObj;
        int totalQuestions = 0;
        int correctQuestions = 0;

        AbilityStat(Long abilityId, Ability abilityObj) {
            this.abilityId = abilityId;
            this.abilityObj = abilityObj;
        }
    }
}
