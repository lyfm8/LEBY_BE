package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.AbilityRadarResponse;
import com.example.be.dto.response.diagnostic.DiagnosticAiRoadmapSummaryResponse;
import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.dto.response.diagnostic.PartAbilityBreakdownResponse;
import com.example.be.dto.response.diagnostic.PartResultDetailResponse;
import com.example.be.dto.response.diagnostic.SectionScoresResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi kết quả lượt thi chẩn đoán (DiagnosticAttempt, PartDiagnosticResult, UserAbility)
 * sang DTO báo cáo toàn diện phục vụ màn hình Xem báo cáo chẩn đoán và Radar năng lực (UC07).
 */
@Component
public class DiagnosticResultMapper {

    private static final int TOEIC_MAX_SCORE = 990;
    private static final int TOEIC_SECTION_MAX_SCORE = 495;
    private static final int SCORE_ROUND_STEP = 5;
    private static final int MAX_PART_SCORE_PERCENT = 100;
    private static final float DEFAULT_CONFIRM_CUTOFF = 50.0f;
    private static final float DEFAULT_STABLE_CUTOFF = 75.0f;
    private static final Pattern PART_NUMBER_PATTERN = Pattern.compile("Part\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Chuyển đổi toàn bộ dữ liệu kết quả chẩn đoán sang {@link DiagnosticResultDetailResponse}.
     *
     * @param attempt          Lượt thi chẩn đoán đã nộp
     * @param partResults      Danh sách kết quả 7 Part
     * @param answers          Danh sách câu trả lời của học viên trong lượt thi
     * @param examQuestions    Toàn bộ câu hỏi của đề thi (đã nạp Part và QuestionAbilities)
     * @param userAbilities    Snapshot năng lực UserAbility của học viên
     * @param abilityRules     Quy tắc đánh giá Ability cấu hình trong DB
     * @param targetProfile    Mục tiêu AIM gắn với lượt thi / người dùng
     * @param learningPath     Lộ trình học thích ứng sinh ra từ lượt thi này (có thể null)
     * @return Báo cáo kết quả chi tiết phục vụ UC07
     */
    public DiagnosticResultDetailResponse toDetailResponse(
            DiagnosticAttempt attempt,
            List<PartDiagnosticResult> partResults,
            List<DiagnosticAnswer> answers,
            List<Question> examQuestions,
            List<UserAbility> userAbilities,
            List<AbilityEvaluationRule> abilityRules,
            TargetProfile targetProfile,
            LearningPath learningPath
    ) {
        boolean hasViewed = attempt != null && Boolean.TRUE.equals(attempt.getHasViewedReview());
        return toDetailResponse(
                attempt,
                partResults,
                answers,
                examQuestions,
                userAbilities,
                abilityRules,
                targetProfile,
                learningPath,
                hasViewed
        );
    }

    /**
     * Phiên bản đầy đủ kèm cờ {@code hasAnyReviewedAttempt} cấp tài khoản để kiểm soát khóa quyền làm lại.
     */
    public DiagnosticResultDetailResponse toDetailResponse(
            DiagnosticAttempt attempt,
            List<PartDiagnosticResult> partResults,
            List<DiagnosticAnswer> answers,
            List<Question> examQuestions,
            List<UserAbility> userAbilities,
            List<AbilityEvaluationRule> abilityRules,
            TargetProfile targetProfile,
            LearningPath learningPath,
            boolean hasAnyReviewedAttempt
    ) {
        // Bước 1: Lập chỉ mục câu trả lời theo questionId để tra cứu O(1)
        Map<Long, DiagnosticAnswer> answerByQuestionId = (answers != null ? answers : Collections.<DiagnosticAnswer>emptyList())
                .stream()
                .filter(a -> a.getQuestion() != null)
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a,
                        (first, second) -> first
                ));

        Map<Long, UserAbility> userAbilityByAbilityId = (userAbilities != null ? userAbilities : Collections.<UserAbility>emptyList())
                .stream()
                .filter(ua -> ua.getAbility() != null)
                .collect(Collectors.toMap(
                        ua -> ua.getAbility().getId(),
                        ua -> ua,
                        (first, second) -> first
                ));

        Map<Long, AbilityEvaluationRule> ruleByAbilityId = (abilityRules != null ? abilityRules : Collections.<AbilityEvaluationRule>emptyList())
                .stream()
                .filter(r -> r.getAbility() != null)
                .collect(Collectors.toMap(
                        r -> r.getAbility().getId(),
                        r -> r,
                        (first, second) -> first
                ));

        // Bước 2: Nhóm toàn bộ câu hỏi của đề theo Part để tính số câu đúng/tổng số câu chính xác theo từng Part và Listening/Reading
        List<Question> safeQuestions = examQuestions != null ? examQuestions : Collections.emptyList();
        Map<Long, List<Question>> questionsByPartId = new LinkedHashMap<>();
        for (Question q : safeQuestions) {
            if (q != null && q.getPart() != null) {
                questionsByPartId.computeIfAbsent(q.getPart().getId(), k -> new ArrayList<>()).add(q);
            }
        }

        int totalQuestions = !safeQuestions.isEmpty() ? safeQuestions.size() : answerByQuestionId.size();
        int totalCorrect = !safeQuestions.isEmpty()
                ? (int) safeQuestions.stream().filter(q -> isQuestionCorrect(q, answerByQuestionId)).count()
                : (int) answerByQuestionId.values().stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

        // Bước 3: Tính điểm dự đoán chuẩn TOEIC thang 990 (chia hết cho 5) và điểm 2 phần Listening / Reading
        int predictedScore = calculateScaledToeicScore(totalCorrect, totalQuestions, TOEIC_MAX_SCORE);
        SectionScoresResponse sectionScores = buildSectionScores(safeQuestions, answerByQuestionId);

        // Bước 4: Ánh xạ chi tiết 7 Part, biểu đồ Radar 7 chiều, Ability cần bồi dưỡng và Part miễn học
        List<PartDiagnosticResult> sortedPartResults = new ArrayList<>(partResults != null ? partResults : Collections.emptyList());
        sortedPartResults.sort(Comparator.comparingInt(pr -> resolvePartNumber(pr.getPart())));

        List<PartResultDetailResponse> partDetailResponses = new ArrayList<>();
        List<AbilityRadarResponse> radarResponses = new ArrayList<>();
        List<PartAbilityBreakdownResponse> weakAbilities = new ArrayList<>();
        List<String> passedParts = new ArrayList<>();
        List<Integer> focusParts = new ArrayList<>();

        for (PartDiagnosticResult pr : sortedPartResults) {
            Part part = pr.getPart();
            int partNo = resolvePartNumber(part);
            String partName = part != null && part.getName() != null ? part.getName() : "Part " + partNo;
            Long partId = part != null ? part.getId() : (long) partNo;

            List<Question> partQuestions = questionsByPartId.getOrDefault(partId, Collections.emptyList());
            int partTotal = partQuestions.size();
            int partCorrect = (int) partQuestions.stream().filter(q -> isQuestionCorrect(q, answerByQuestionId)).count();
            int scorePercent = Math.round(pr.getScore() != null ? pr.getScore() : (partTotal > 0 ? (partCorrect * 100.0f) / partTotal : 0.0f));
            scorePercent = Math.max(0, Math.min(MAX_PART_SCORE_PERCENT, scorePercent));

            List<PartAbilityBreakdownResponse> partAbilities = buildPartAbilityBreakdowns(
                    partNo,
                    partName,
                    partQuestions,
                    answerByQuestionId,
                    pr.getDirective(),
                    userAbilityByAbilityId,
                    ruleByAbilityId
            );

            partDetailResponses.add(PartResultDetailResponse.builder()
                    .partId(partId)
                    .partNo(partNo)
                    .name(partName)
                    .correctCount(partCorrect)
                    .totalQuestions(partTotal)
                    .score(scorePercent)
                    .maxScore(MAX_PART_SCORE_PERCENT)
                    .directive(pr.getDirective().name())
                    .directiveLabel(resolveDirectiveLabel(pr.getDirective()))
                    .abilities(partAbilities)
                    .build());

            radarResponses.add(AbilityRadarResponse.builder()
                    .partNo(partNo)
                    .code("PART_" + partNo)
                    .name("Part " + partNo)
                    .score(scorePercent)
                    .status(mapDirectiveToRadarStatus(pr.getDirective()).name())
                    .build());

            if (pr.getDirective() == EPartDirective.PASS) {
                passedParts.add(partName);
            } else {
                focusParts.add(partNo);
            }

            for (PartAbilityBreakdownResponse ab : partAbilities) {
                if (!EAbilityStatus.STABLE.name().equals(ab.getStatus())) {
                    weakAbilities.add(ab);
                }
            }
        }

        // Bước 5: Tổng hợp thông điệp khuyến nghị sư phạm tùy biến theo mục tiêu AIM
        Integer targetScore = targetProfile != null ? targetProfile.getTargetTotalScore() : null;
        boolean hasGeneratedRoadmap = learningPath != null;
        int moduleCount = hasGeneratedRoadmap && learningPath.getItems() != null
                ? learningPath.getItems().size()
                : 0;

        DiagnosticAiRoadmapSummaryResponse roadmapSummary = buildRoadmapSummary(
                predictedScore,
                targetScore,
                sectionScores,
                focusParts,
                passedParts,
                moduleCount,
                hasGeneratedRoadmap
        );

        boolean hasViewedThisAttempt = Boolean.TRUE.equals(attempt.getHasViewedReview()) || hasAnyReviewedAttempt;

        return DiagnosticResultDetailResponse.builder()
                .attemptId(attempt.getId())
                .testTitle(attempt.getDiagnosticTest() != null ? attempt.getDiagnosticTest().getTitle() : "Bài thi chẩn đoán năng lực")
                .completedAt(attempt.getCompletedAt())
                .learningPathId(learningPath != null ? learningPath.getId() : null)
                .predictedScore(predictedScore)
                .targetScore(targetScore)
                .totalQuestions(totalQuestions)
                .correctAnswers(totalCorrect)
                .durationSeconds(attempt.getDurationSeconds())
                .sectionScores(sectionScores)
                .partResults(partDetailResponses)
                .abilitiesRadar(radarResponses)
                .weakAbilities(weakAbilities)
                .passedParts(passedParts)
                .aiRoadmapSummary(roadmapSummary)
                .hasViewedReview(hasViewedThisAttempt)
                .canRetakeDiagnostic(!hasAnyReviewedAttempt)
                .build();
    }

    /**
     * Quy đổi số câu đúng trên tổng số câu sang thang điểm chuẩn TOEIC (990 hoặc 495),
     * luôn làm tròn về bội số của 5 theo đúng đặc tả đề thi TOEIC.
     */
    public int calculateScaledToeicScore(int correct, int total, int maxScale) {
        if (total <= 0 || correct <= 0) {
            return 0;
        }
        double rawScore = ((double) correct / total) * maxScale;
        int rounded = (int) (Math.round(rawScore / SCORE_ROUND_STEP) * SCORE_ROUND_STEP);
        return Math.max(0, Math.min(maxScale, rounded));
    }

    private SectionScoresResponse buildSectionScores(
            List<Question> examQuestions,
            Map<Long, DiagnosticAnswer> answerByQuestionId
    ) {
        int listeningTotal = 0;
        int listeningCorrect = 0;
        int readingTotal = 0;
        int readingCorrect = 0;

        for (Question q : examQuestions) {
            if (q == null) {
                continue;
            }
            int partNo = resolvePartNumber(q.getPart());
            boolean isListening = partNo >= 1 && partNo <= 4;
            boolean correct = isQuestionCorrect(q, answerByQuestionId);

            if (isListening) {
                listeningTotal++;
                if (correct) {
                    listeningCorrect++;
                }
            } else {
                readingTotal++;
                if (correct) {
                    readingCorrect++;
                }
            }
        }

        int listeningPercent = listeningTotal > 0 ? Math.round((listeningCorrect * 100.0f) / listeningTotal) : 0;
        int readingPercent = readingTotal > 0 ? Math.round((readingCorrect * 100.0f) / readingTotal) : 0;

        return SectionScoresResponse.builder()
                .listening(calculateScaledToeicScore(listeningCorrect, listeningTotal, TOEIC_SECTION_MAX_SCORE))
                .reading(calculateScaledToeicScore(readingCorrect, readingTotal, TOEIC_SECTION_MAX_SCORE))
                .listeningCorrect(listeningCorrect)
                .listeningTotal(listeningTotal)
                .readingCorrect(readingCorrect)
                .readingTotal(readingTotal)
                .listeningPercent(listeningPercent)
                .readingPercent(readingPercent)
                .build();
    }

    private List<PartAbilityBreakdownResponse> buildPartAbilityBreakdowns(
            int partNo,
            String partName,
            List<Question> partQuestions,
            Map<Long, DiagnosticAnswer> answerByQuestionId,
            EPartDirective partDirective,
            Map<Long, UserAbility> userAbilityByAbilityId,
            Map<Long, AbilityEvaluationRule> ruleByAbilityId
    ) {
        Map<Long, AbilityBucket> buckets = new LinkedHashMap<>();

        for (Question q : partQuestions) {
            if (q.getQuestionAbilities() == null) {
                continue;
            }
            boolean correct = isQuestionCorrect(q, answerByQuestionId);
            for (QuestionAbility qa : q.getQuestionAbilities()) {
                Ability ability = qa.getAbility();
                if (ability == null) {
                    continue;
                }
                AbilityBucket bucket = buckets.computeIfAbsent(
                        ability.getId(),
                        k -> new AbilityBucket(ability.getId(), ability.getName())
                );
                bucket.total++;
                if (correct) {
                    bucket.correct++;
                }
            }
        }

        List<PartAbilityBreakdownResponse> breakdowns = new ArrayList<>();
        for (AbilityBucket bucket : buckets.values()) {
            float accuracy = bucket.total > 0 ? (bucket.correct * 100.0f) / bucket.total : 0.0f;
            EAbilityStatus status = resolveAbilityStatus(
                    bucket.abilityId,
                    accuracy,
                    partDirective,
                    userAbilityByAbilityId,
                    ruleByAbilityId
            );

            breakdowns.add(PartAbilityBreakdownResponse.builder()
                    .abilityId(bucket.abilityId)
                    .abilityName(bucket.abilityName)
                    .partNo(partNo)
                    .partName(partName)
                    .correctCount(bucket.correct)
                    .totalCount(bucket.total)
                    .accuracyPercent(Math.round(accuracy))
                    .status(status.name())
                    .build());
        }

        return breakdowns;
    }

    private EAbilityStatus resolveAbilityStatus(
            Long abilityId,
            float accuracy,
            EPartDirective partDirective,
            Map<Long, UserAbility> userAbilityByAbilityId,
            Map<Long, AbilityEvaluationRule> ruleByAbilityId
    ) {
        UserAbility persisted = userAbilityByAbilityId.get(abilityId);
        if (persisted != null && persisted.getStatus() != null) {
            return persisted.getStatus();
        }
        if (partDirective == EPartDirective.PASS) {
            return EAbilityStatus.STABLE;
        }
        AbilityEvaluationRule rule = ruleByAbilityId.get(abilityId);
        if (rule != null) {
            return rule.isWeak(accuracy, partDirective) ? EAbilityStatus.WEAK : EAbilityStatus.STABLE;
        }
        float cutoff = partDirective == EPartDirective.CONFIRM ? DEFAULT_CONFIRM_CUTOFF : DEFAULT_STABLE_CUTOFF;
        return accuracy < cutoff ? EAbilityStatus.WEAK : EAbilityStatus.STABLE;
    }

    private EAbilityStatus mapDirectiveToRadarStatus(EPartDirective directive) {
        if (directive == null) {
            return EAbilityStatus.DEVELOPING;
        }
        return switch (directive) {
            case PASS -> EAbilityStatus.STABLE;
            case CONFIRM -> EAbilityStatus.DEVELOPING;
            case ABILITY_DIAGNOSTIC, FULL_PART -> EAbilityStatus.WEAK;
        };
    }

    private String resolveDirectiveLabel(EPartDirective directive) {
        if (directive == null) {
            return "Cần đánh giá";
        }
        return switch (directive) {
            case PASS -> "Đạt chuẩn";
            case CONFIRM -> "Cận chuẩn";
            case ABILITY_DIAGNOSTIC -> "Dưới chuẩn";
            case FULL_PART -> "Mất gốc";
        };
    }

    private DiagnosticAiRoadmapSummaryResponse buildRoadmapSummary(
            int predictedScore,
            Integer targetScore,
            SectionScoresResponse sectionScores,
            List<Integer> focusParts,
            List<String> passedParts,
            int moduleCount,
            boolean hasGeneratedRoadmap
    ) {
        int listeningPercent = sectionScores.getListeningPercent();
        int readingPercent = sectionScores.getReadingPercent();

        // Tách rõ 4 trường hợp so sánh Nghe và Đọc để tránh lỗi logic khi 2 kỹ năng bằng nhau (VD: cùng 0%)
        String balanceNote;
        if (listeningPercent == 0 && readingPercent == 0) {
            balanceNote = "Cả kỹ năng Nghe và Đọc hiện đều ở mức khởi đầu (0%), bạn cần củng cố lại nền tảng toàn diện.";
        } else if (listeningPercent == readingPercent) {
            balanceNote = String.format("Kỹ năng Nghe và Đọc hiện đang cân bằng ở mức %d%%.", listeningPercent);
        } else if (listeningPercent > readingPercent) {
            balanceNote = String.format("Kỹ năng Nghe (%d%%) đang nhỉnh hơn Đọc (%d%%).", listeningPercent, readingPercent);
        } else {
            balanceNote = String.format("Kỹ năng Đọc (%d%%) đang nhỉnh hơn Nghe (%d%%).", readingPercent, listeningPercent);
        }

        String aimNote = targetScore != null
                ? String.format(" So với mục tiêu AIM %d (hiện ước tính %d điểm), ", targetScore, predictedScore)
                : String.format(" Với điểm ước tính hiện tại %d điểm, ", predictedScore);

        String actionNote;
        if (focusParts.isEmpty()) {
            actionNote = "bạn đã đạt chuẩn toàn bộ 7 Part! Hãy tiếp tục duy trì phong độ bằng các bài luyện tập tổng hợp.";
        } else {
            // Rút gọn gọn gàng khi cần ôn cả 7 phần thay vì liệt kê dài Part 1, Part 2, ..., Part 7
            String focusText = focusParts.size() == 7
                    ? "toàn bộ 7 phần thi (Part 1 – Part 7)"
                    : focusParts.stream().map(p -> "Part " + p).collect(Collectors.joining(", "));
            String moduleSuffix = hasGeneratedRoadmap ? String.format(" (%d mô-đun trọng tâm)", moduleCount) : "";
            String passedSuffix = passedParts.isEmpty() ? "" : " và được miễn học " + passedParts.size() + " phần đã đạt chuẩn";
            actionNote = String.format("bạn cần tập trung cải thiện %s%s%s.", focusText, moduleSuffix, passedSuffix);
        }

        return DiagnosticAiRoadmapSummaryResponse.builder()
                .radarInsight(balanceNote + aimNote + actionNote)
                .focusParts(focusParts)
                .moduleCount(moduleCount)
                .recommendedAction(hasGeneratedRoadmap ? "Xem lộ trình học cá nhân của tôi" : "Quay về trang tổng quan")
                .secondaryAction("Tải báo cáo tóm tắt")
                .build();
    }

    private boolean isQuestionCorrect(Question q, Map<Long, DiagnosticAnswer> answerByQuestionId) {
        if (q == null) {
            return false;
        }
        DiagnosticAnswer ans = answerByQuestionId.get(q.getId());
        return ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
    }

    private int resolvePartNumber(Part part) {
        if (part == null) {
            return 1;
        }
        if (part.getName() != null) {
            Matcher matcher = PART_NUMBER_PATTERN.matcher(part.getName());
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // Fallback về ID bên dưới
                }
            }
        }
        return part.getId() != null ? part.getId().intValue() : 1;
    }

    private static class AbilityBucket {
        final Long abilityId;
        final String abilityName;
        int total = 0;
        int correct = 0;

        AbilityBucket(Long abilityId, String abilityName) {
            this.abilityId = abilityId;
            this.abilityName = abilityName;
        }
    }
}
