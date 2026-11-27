package com.example.be.mapper.mockexam;

import com.example.be.dto.response.diagnostic.AbilityRadarResponse;
import com.example.be.dto.response.diagnostic.PartAbilityBreakdownResponse;
import com.example.be.dto.response.mockexam.MockExamPartComparisonItem;
import com.example.be.dto.response.mockexam.MockExamReportResponse;
import com.example.be.dto.response.mockexam.MockExamSubmitResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.entity.setting.TargetPartThreshold;
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.mapper.question.ExamQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper chuyên biệt tính điểm chuẩn ETS TOEIC (10 - 990) từ {@link ToeicScoreConversion}
 * và kết xuất Báo cáo nghiệm thu đầu ra Trước vs Sau (UC18 - BR15).
 */
@Component
@RequiredArgsConstructor
public class MockExamReportMapper {

    private static final int STANDARD_SECTION_QUESTIONS = 100;
    private static final int TOEIC_SECTION_MIN_SCORE = 5;
    private static final int TOEIC_SECTION_MAX_SCORE = 495;
    private static final int SCORE_ROUND_STEP = 5;
    private static final int MAX_PART_PERCENT = 100;
    private static final int TOTAL_TOEIC_PARTS = 7;
    private static final int LISTENING_PART_MAX = 4;
    private static final float DEFAULT_CONFIRM_CUTOFF = 50.0f;
    private static final float DEFAULT_STABLE_CUTOFF = 75.0f;
    private static final int MAX_RECOMMENDATIONS = 5;

    private final ExamQuestionMapper examQuestionMapper;

    /**
     * Xác định lượt thi thử có được phép mở xem đáp án và lời giải chi tiết hay không:
     * đề luyện tập (PRACTICE) luôn cho xem; đề đầu ra (OUTPUT) chỉ mở khi đạt mục tiêu AIM.
     */
    public static boolean isReviewUnlocked(EExamTag examTag, int totalToeicScore, Integer targetScore) {
        if (examTag != EExamTag.OUTPUT || targetScore == null) {
            return true;
        }
        return totalToeicScore >= targetScore;
    }

    /**
     * Quy đổi số câu trả lời đúng của một kỹ năng (Listening hoặc Reading) sang thang điểm chuẩn ETS 5 - 495.
     */
    public int resolveSectionToeicScore(
            int correctCount,
            int totalQuestions,
            boolean isListening,
            Map<Integer, ToeicScoreConversion> conversionMap
    ) {
        if (totalQuestions <= 0) {
            return 0;
        }
        int lookupKey = (totalQuestions == STANDARD_SECTION_QUESTIONS)
                ? correctCount
                : Math.round((correctCount * (float) STANDARD_SECTION_QUESTIONS) / totalQuestions);
        lookupKey = Math.max(0, Math.min(STANDARD_SECTION_QUESTIONS, lookupKey));

        ToeicScoreConversion conversion = conversionMap != null ? conversionMap.get(lookupKey) : null;
        if (conversion != null) {
            return isListening ? conversion.getListeningScore() : conversion.getReadingScore();
        }

        // Fallback theo tỷ lệ chia hết cho 5 khi bảng quy đổi chưa được nạp trong môi trường test
        if (correctCount <= 0) {
            return TOEIC_SECTION_MIN_SCORE;
        }
        double raw = ((double) correctCount / totalQuestions) * TOEIC_SECTION_MAX_SCORE;
        int rounded = (int) (Math.round(raw / SCORE_ROUND_STEP) * SCORE_ROUND_STEP);
        return Math.max(TOEIC_SECTION_MIN_SCORE, Math.min(TOEIC_SECTION_MAX_SCORE, rounded));
    }

    /**
     * Ánh xạ kết quả tóm tắt ngay sau khi nộp bài thi thử TOEIC 200 câu (UC17).
     */
    public MockExamSubmitResponse toSubmitResponse(
            DiagnosticAttempt attempt,
            DiagnosticTest test,
            int listeningScore,
            int readingScore,
            int totalCorrect,
            int totalQuestions,
            Integer targetScore
    ) {
        int totalToeicScore = listeningScore + readingScore;
        EExamTag tag = test != null && test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE;
        Boolean isAimAchieved = (tag == EExamTag.OUTPUT && targetScore != null)
                ? totalToeicScore >= targetScore
                : null;

        return MockExamSubmitResponse.builder()
                .attemptId(attempt.getId())
                .testId(test != null ? test.getId() : null)
                .examTag(tag)
                .listeningScore(listeningScore)
                .readingScore(readingScore)
                .totalToeicScore(totalToeicScore)
                .totalCorrect(totalCorrect)
                .totalQuestions(totalQuestions)
                .isAimAchieved(isAimAchieved)
                .build();
    }

    /**
     * Ánh xạ dữ liệu hoàn chỉnh sang {@link MockExamReportResponse}.
     * - Với đề {@code PRACTICE}: chỉ trả điểm quy đổi 990 và thống kê 7 Part cơ bản (không có Radar Trước vs Sau).
     * - Với đề {@code OUTPUT}: kết xuất đầy đủ Radar Trước vs Sau (BR15), đối sánh từng Part, Ability và khuyến nghị UC18.
     */
    public MockExamReportResponse toReportResponse(
            DiagnosticAttempt mockAttempt,
            List<PartDiagnosticResult> afterPartResults,
            List<DiagnosticAnswer> mockAnswers,
            List<Question> examQuestions,
            List<PartDiagnosticResult> beforePartResults,
            List<AbilityEvaluationRule> abilityRules,
            Map<Long, TargetPartThreshold> thresholdByPartId,
            Map<Integer, ToeicScoreConversion> conversionMap,
            TargetProfile targetProfile
    ) {
        DiagnosticTest test = mockAttempt.getDiagnosticTest();
        EExamTag examTag = test != null && test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE;
        boolean isOutputExam = examTag == EExamTag.OUTPUT;

        // Bước 1: Lập chỉ mục câu trả lời theo questionId để tra cứu O(1)
        Map<Long, DiagnosticAnswer> answerByQuestionId = indexAnswersByQuestionId(mockAnswers);
        Map<Long, AbilityEvaluationRule> ruleByAbilityId = indexRulesByAbilityId(abilityRules);

        // Bước 2: Gom câu hỏi theo từng Part và thống kê đúng/sai Listening & Reading
        List<Question> safeQuestions = examQuestions != null ? examQuestions : Collections.emptyList();
        Map<Integer, List<Question>> questionsByPartNo = new LinkedHashMap<>();
        int listeningTotal = 0;
        int listeningCorrect = 0;
        int readingTotal = 0;
        int readingCorrect = 0;

        for (Question q : safeQuestions) {
            if (q == null) {
                continue;
            }
            int partNo = examQuestionMapper.resolvePartNumber(q.getPart());
            questionsByPartNo.computeIfAbsent(partNo, k -> new ArrayList<>()).add(q);

            boolean correct = isQuestionCorrect(q, answerByQuestionId);
            if (partNo <= LISTENING_PART_MAX) {
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

        int totalQuestions = !safeQuestions.isEmpty() ? safeQuestions.size() : answerByQuestionId.size();
        int totalCorrect = listeningCorrect + readingCorrect;

        // Bước 3: Quy đổi điểm chuẩn ETS 5 - 495 cho Listening & Reading và tổng 10 - 990
        int listeningScore = resolveSectionToeicScore(listeningCorrect, listeningTotal, true, conversionMap);
        int readingScore = resolveSectionToeicScore(readingCorrect, readingTotal, false, conversionMap);
        int totalToeicScore = listeningScore + readingScore;

        int listeningPercent = listeningTotal > 0 ? Math.round((listeningCorrect * 100.0f) / listeningTotal) : 0;
        int readingPercent = readingTotal > 0 ? Math.round((readingCorrect * 100.0f) / readingTotal) : 0;

        Integer targetScore = targetProfile != null ? targetProfile.getTargetTotalScore() : null;
        Boolean isAimAchieved = (isOutputExam && targetScore != null) ? totalToeicScore >= targetScore : null;
        boolean canViewReview = isReviewUnlocked(examTag, totalToeicScore, targetScore);
        Integer scoreDifference = (isOutputExam && targetScore != null) ? totalToeicScore - targetScore : null;

        // Bước 4: Chuẩn bị dữ liệu đối sánh Trước (chẩn đoán đầu vào) nếu là đề thi đầu ra OUTPUT
        Map<Integer, PartDiagnosticResult> afterByPartNo = indexPartResultsByPartNo(afterPartResults);
        Map<Integer, PartDiagnosticResult> beforeByPartNo = isOutputExam
                ? indexPartResultsByPartNo(beforePartResults)
                : Collections.emptyMap();

        List<MockExamPartComparisonItem> partComparisonItems = new ArrayList<>();
        List<AbilityRadarResponse> beforeRadar = isOutputExam ? new ArrayList<>() : null;
        List<AbilityRadarResponse> afterRadar = isOutputExam ? new ArrayList<>() : null;
        List<PartAbilityBreakdownResponse> remainingWeakAbilities = new ArrayList<>();

        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
            PartDiagnosticResult afterPr = afterByPartNo.get(partNo);
            List<Question> partQuestions = questionsByPartNo.getOrDefault(partNo, Collections.emptyList());

            Part partEntity = resolvePartEntity(afterPr, partQuestions);
            Long partId = partEntity != null ? partEntity.getId() : (long) partNo;
            String partName = partEntity != null && partEntity.getName() != null ? partEntity.getName() : "Part " + partNo;

            int partTotal = partQuestions.size();
            int partCorrect = (int) partQuestions.stream()
                    .filter(q -> isQuestionCorrect(q, answerByQuestionId))
                    .count();
            int afterScorePercent = resolvePartScorePercent(afterPr, partCorrect, partTotal);

            Integer beforeScorePercent = null;
            Integer delta = null;
            List<PartAbilityBreakdownResponse> partAbilities = Collections.emptyList();

            if (isOutputExam) {
                PartDiagnosticResult beforePr = beforeByPartNo.get(partNo);
                beforeScorePercent = beforePr != null && beforePr.getScore() != null
                        ? clampPercent(Math.round(beforePr.getScore()))
                        : 0;
                delta = afterScorePercent - beforeScorePercent;

                TargetPartThreshold threshold = thresholdByPartId != null ? thresholdByPartId.get(partId) : null;
                EPartDirective effectiveDirective = threshold != null
                        ? threshold.determineDirective((float) afterScorePercent)
                        : (afterPr != null && afterPr.getDirective() != null
                                ? afterPr.getDirective()
                                : EPartDirective.ABILITY_DIAGNOSTIC);

                partAbilities = buildPartAbilityBreakdowns(
                        partNo,
                        partName,
                        partQuestions,
                        answerByQuestionId,
                        effectiveDirective,
                        ruleByAbilityId
                );

                for (PartAbilityBreakdownResponse ab : partAbilities) {
                    if (!EAbilityStatus.STABLE.name().equals(ab.getStatus())) {
                        remainingWeakAbilities.add(ab);
                    }
                }

                beforeRadar.add(AbilityRadarResponse.builder()
                        .partNo(partNo)
                        .code("PART_" + partNo)
                        .name("Part " + partNo)
                        .score(beforeScorePercent)
                        .status(mapPartPercentToRadarStatus(beforeScorePercent).name())
                        .build());

                afterRadar.add(AbilityRadarResponse.builder()
                        .partNo(partNo)
                        .code("PART_" + partNo)
                        .name("Part " + partNo)
                        .score(afterScorePercent)
                        .status(mapDirectiveToRadarStatus(effectiveDirective).name())
                        .build());
            }

            partComparisonItems.add(MockExamPartComparisonItem.builder()
                    .partId(partId)
                    .partNo(partNo)
                    .name(partName)
                    .correctCount(partCorrect)
                    .totalQuestions(partTotal)
                    .score(afterScorePercent)
                    .beforeScore(beforeScorePercent)
                    .scoreDelta(delta)
                    .abilities(partAbilities)
                    .build());
        }

        // Bước 5: Sắp xếp điểm yếu còn sót lại theo tỷ lệ đúng tăng dần và tạo khuyến nghị UC18 (chỉ cho đề OUTPUT)
        String summaryHeadline = null;
        String summaryMessage = null;
        List<String> recommendations = Collections.emptyList();

        if (isOutputExam) {
            remainingWeakAbilities.sort(Comparator.comparingInt(PartAbilityBreakdownResponse::getAccuracyPercent));
            OutputVerdict verdict = buildOutputVerdict(
                    totalToeicScore,
                    targetScore,
                    listeningScore,
                    readingScore,
                    partComparisonItems,
                    remainingWeakAbilities
            );
            summaryHeadline = verdict.headline();
            summaryMessage = verdict.message();
            recommendations = verdict.recommendations();
        }

        return MockExamReportResponse.builder()
                .attemptId(mockAttempt.getId())
                .testId(test != null ? test.getId() : null)
                .testTitle(test != null && test.getTitle() != null ? test.getTitle() : "Đề thi thử TOEIC chuẩn ETS")
                .examTag(examTag)
                .completedAt(mockAttempt.getCompletedAt())
                .durationSeconds(mockAttempt.getDurationSeconds())
                .listeningScore(listeningScore)
                .readingScore(readingScore)
                .totalToeicScore(totalToeicScore)
                .listeningCorrect(listeningCorrect)
                .listeningTotal(listeningTotal)
                .listeningPercent(listeningPercent)
                .readingCorrect(readingCorrect)
                .readingTotal(readingTotal)
                .readingPercent(readingPercent)
                .totalCorrect(totalCorrect)
                .totalQuestions(totalQuestions)
                .targetScore(isOutputExam ? targetScore : null)
                .isAimAchieved(isAimAchieved)
                .canViewReview(canViewReview)
                .scoreDifference(scoreDifference)
                .beforeRadar(beforeRadar)
                .afterRadar(afterRadar)
                .partResults(partComparisonItems)
                .remainingWeakAbilities(remainingWeakAbilities)
                .recommendations(recommendations)
                .summaryHeadline(summaryHeadline)
                .summaryMessage(summaryMessage)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Map<Long, DiagnosticAnswer> indexAnswersByQuestionId(List<DiagnosticAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyMap();
        }
        return answers.stream()
                .filter(a -> a.getQuestion() != null)
                .collect(Collectors.toMap(
                        a -> a.getQuestion().getId(),
                        a -> a,
                        (first, second) -> first
                ));
    }

    private Map<Long, AbilityEvaluationRule> indexRulesByAbilityId(List<AbilityEvaluationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyMap();
        }
        return rules.stream()
                .filter(r -> r.getAbility() != null)
                .collect(Collectors.toMap(
                        r -> r.getAbility().getId(),
                        r -> r,
                        (first, second) -> first
                ));
    }

    private Map<Integer, PartDiagnosticResult> indexPartResultsByPartNo(List<PartDiagnosticResult> partResults) {
        if (partResults == null || partResults.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, PartDiagnosticResult> map = new HashMap<>();
        for (PartDiagnosticResult pr : partResults) {
            if (pr != null && pr.getPart() != null) {
                int partNo = examQuestionMapper.resolvePartNumber(pr.getPart());
                map.putIfAbsent(partNo, pr);
            }
        }
        return map;
    }

    private Part resolvePartEntity(PartDiagnosticResult pr, List<Question> partQuestions) {
        if (pr != null && pr.getPart() != null) {
            return pr.getPart();
        }
        if (partQuestions != null && !partQuestions.isEmpty()) {
            return partQuestions.get(0).getPart();
        }
        return null;
    }

    private int resolvePartScorePercent(PartDiagnosticResult pr, int correct, int total) {
        if (total > 0) {
            return clampPercent(Math.round((correct * 100.0f) / total));
        }
        if (pr != null && pr.getScore() != null) {
            return clampPercent(Math.round(pr.getScore()));
        }
        return 0;
    }

    private int clampPercent(int raw) {
        return Math.max(0, Math.min(MAX_PART_PERCENT, raw));
    }

    private boolean isQuestionCorrect(Question q, Map<Long, DiagnosticAnswer> answerByQuestionId) {
        if (q == null) {
            return false;
        }
        DiagnosticAnswer ans = answerByQuestionId.get(q.getId());
        return ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
    }

    private List<PartAbilityBreakdownResponse> buildPartAbilityBreakdowns(
            int partNo,
            String partName,
            List<Question> partQuestions,
            Map<Long, DiagnosticAnswer> answerByQuestionId,
            EPartDirective partDirective,
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
            EAbilityStatus status = resolveMockAbilityStatus(bucket.abilityId, accuracy, partDirective, ruleByAbilityId);

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

    private EAbilityStatus resolveMockAbilityStatus(
            Long abilityId,
            float accuracy,
            EPartDirective partDirective,
            Map<Long, AbilityEvaluationRule> ruleByAbilityId
    ) {
        AbilityEvaluationRule rule = ruleByAbilityId.get(abilityId);
        // Nếu Part đã đạt PASS nhưng một Ability con vẫn thấp hơn ngưỡng phát triển -> vẫn cảnh báo WEAK để ôn chặng cuối
        EPartDirective effectiveDirective = (partDirective == EPartDirective.PASS)
                ? EPartDirective.CONFIRM
                : partDirective;

        if (rule != null) {
            return rule.isWeak(accuracy, effectiveDirective) ? EAbilityStatus.WEAK : EAbilityStatus.STABLE;
        }
        float cutoff = effectiveDirective == EPartDirective.CONFIRM ? DEFAULT_CONFIRM_CUTOFF : DEFAULT_STABLE_CUTOFF;
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

    private EAbilityStatus mapPartPercentToRadarStatus(int scorePercent) {
        if (scorePercent >= DEFAULT_STABLE_CUTOFF) {
            return EAbilityStatus.STABLE;
        }
        if (scorePercent >= DEFAULT_CONFIRM_CUTOFF) {
            return EAbilityStatus.DEVELOPING;
        }
        return EAbilityStatus.WEAK;
    }

    private OutputVerdict buildOutputVerdict(
            int totalScore,
            Integer targetScore,
            int listeningScore,
            int readingScore,
            List<MockExamPartComparisonItem> parts,
            List<PartAbilityBreakdownResponse> weakAbilities
    ) {
        boolean achieved = targetScore != null && totalScore >= targetScore;
        String headline;
        String message;

        if (achieved) {
            headline = String.format("Chúc mừng! Bạn đã đạt mục tiêu AIM %d điểm TOEIC", targetScore);
            message = String.format(
                    "Tổng điểm TOEIC ước lượng của bạn đạt %d/990 (Listening %d/495, Reading %d/495), vượt ngưỡng mục tiêu +%d điểm. Bạn đã sẵn sàng bước vào kỳ thi thực tế hoặc đặt mốc AIM cao hơn!",
                    totalScore, listeningScore, readingScore, totalScore - targetScore
            );
        } else if (targetScore != null) {
            int gap = targetScore - totalScore;
            headline = String.format("Chỉ còn cách mục tiêu AIM %d đúng %d điểm — đừng bỏ cuộc!", targetScore, gap);
            message = String.format(
                    "Bạn đạt %d/990 (Listening %d/495, Reading %d/495). Hãy xem biểu đồ đối sánh Trước & Sau để tập trung củng cố thêm các Part và kỹ năng còn hổng trước ngày thi thật.",
                    totalScore, listeningScore, readingScore
            );
        } else {
            headline = String.format("Hoàn thành bài thi thử đầu ra: %d/990 điểm", totalScore);
            message = String.format(
                    "Kết quả quy đổi chuẩn ETS: Listening %d/495, Reading %d/495.",
                    listeningScore, readingScore
            );
        }

        final List<String> recommendations = new ArrayList<>();

        // Khuyến nghị 1: Nhận xét cân bằng Nghe vs Đọc
        if (Math.abs(listeningScore - readingScore) >= 60) {
            if (listeningScore < readingScore) {
                recommendations.add(String.format(
                        "Kỹ năng Listening (%d/495) đang thấp hơn Reading (%d/495). Nên ưu tiên luyện nghe Part 3 và Part 4 hàng ngày theo tốc độ thật.",
                        listeningScore, readingScore
                ));
            } else {
                recommendations.add(String.format(
                        "Kỹ năng Reading (%d/495) đang thấp hơn Listening (%d/495). Hãy phân bổ lại thời gian làm bài cho Part 5-6 (tối đa 18 phút) để đủ giờ xử lý Part 7.",
                        readingScore, listeningScore
                ));
            }
        }

        // Khuyến nghị 2: Nêu rõ các Part tiến bộ tốt và các Part giảm/thấp nhất
        parts.stream()
                .filter(p -> p.getScoreDelta() != null && p.getScoreDelta() > 0)
                .max(Comparator.comparingInt(MockExamPartComparisonItem::getScoreDelta))
                .ifPresent(best -> recommendations.add(String.format(
                        "%s là phần có bước tiến vượt bậc nhất sau lộ trình: %d%% -> %d%% (+%d%%).",
                        best.getName(), best.getBeforeScore(), best.getScore(), best.getScoreDelta()
                )));

        // Khuyến nghị 3: Các Ability yếu còn sót lại
        for (int i = 0; i < Math.min(3, weakAbilities.size()); i++) {
            PartAbilityBreakdownResponse ab = weakAbilities.get(i);
            recommendations.add(String.format(
                    "Cần ôn tập trọng tâm kỹ năng \"%s\" ở Part %d (hiện đúng %d/%d câu - %d%%).",
                    ab.getAbilityName(), ab.getPartNo(), ab.getCorrectCount(), ab.getTotalCount(), ab.getAccuracyPercent()
            ));
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Phong độ của bạn rất đồng đều ở cả 7 Part! Hãy giữ nhịp làm đề luyện tập mỗi tuần trước kỳ thi TOEIC chính thức.");
        }

        List<String> finalRecommendations = recommendations.size() > MAX_RECOMMENDATIONS
                ? recommendations.subList(0, MAX_RECOMMENDATIONS)
                : recommendations;

        return new OutputVerdict(headline, message, finalRecommendations);
    }

    private record OutputVerdict(String headline, String message, List<String> recommendations) {
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
