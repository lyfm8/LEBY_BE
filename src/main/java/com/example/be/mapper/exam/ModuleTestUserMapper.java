package com.example.be.mapper.exam;

import com.example.be.dto.response.exam.ModuleTestDraftAnswerResponse;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse.AiFeedbackResponse;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse.AttemptHistoryItemResponse;
import com.example.be.dto.response.exam.ModuleTestPerformanceReportResponse.MetricSummaryResponse;
import com.example.be.dto.response.exam.ModuleTestQuestionSnapshotItem;
import com.example.be.dto.response.exam.ModuleTestReviewQuestionResponse;
import com.example.be.dto.response.exam.ModuleTestReviewResponse;
import com.example.be.dto.response.exam.ModuleTestRoomQuestionResponse;
import com.example.be.dto.response.exam.ModuleTestRoomResponse;
import com.example.be.dto.response.exam.StartModuleTestResponse;
import com.example.be.dto.response.exam.SubmitModuleTestResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleTestAnswer;
import com.example.be.entity.content.ModuleTestAttempt;
import com.example.be.entity.content.Part;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.ETestResult;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.mapper.question.ExamQuestionMapper.MappedQuestionContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyên biệt chuyển đổi dữ liệu Phòng thi vượt ải, Chấm điểm, Báo cáo kết quả
 * và Xem lại bài thi Module Test (UC13 - UC14). Tái sử dụng {@link ExamQuestionMapper}.
 */
@Component
@RequiredArgsConstructor
public class ModuleTestUserMapper {

    private static final int MAX_SCORE = 100;
    private static final int DEFAULT_DURATION_MINUTES = 25;
    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ExamQuestionMapper examQuestionMapper;

    /**
     * Chuyển đổi lượt thi vừa khởi tạo sang {@link StartModuleTestResponse}.
     */
    public StartModuleTestResponse toStartResponse(ModuleTestAttempt attempt, Module module) {
        int durationMinutes = resolveDurationMinutes(module);
        return StartModuleTestResponse.builder()
                .attemptId(attempt.getId())
                .moduleId(module.getId())
                .startedAt(attempt.getStartedAt())
                .durationMinutes(durationMinutes)
                .build();
    }

    /**
     * Chuyển đổi toàn bộ dữ liệu phòng thi vượt ải sang {@link ModuleTestRoomResponse}.
     */
    public ModuleTestRoomResponse toRoomResponse(
            Module module,
            ModuleTestAttempt attempt,
            int passScore,
            int targetScore,
            long remainingSeconds,
            List<ModuleTestQuestionSnapshotItem> snapshotItems,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, ModuleTestAnswer> draftAnswerByQuestionId
    ) {
        List<ModuleTestQuestionSnapshotItem> safeSnapshot = snapshotItems != null
                ? snapshotItems.stream()
                        .sorted(Comparator.comparing(ModuleTestQuestionSnapshotItem::getOrderNo))
                        .toList()
                : Collections.emptyList();

        List<ModuleTestRoomQuestionResponse> questionResponses = new ArrayList<>(safeSnapshot.size());
        List<ModuleTestDraftAnswerResponse> draftResponses = new ArrayList<>();

        for (ModuleTestQuestionSnapshotItem item : safeSnapshot) {
            Question detailed = detailedQuestionMap.get(item.getQuestionId());
            if (detailed == null) {
                continue;
            }
            questionResponses.add(toRoomQuestion(item, detailed));

            ModuleTestAnswer draft = draftAnswerByQuestionId != null
                    ? draftAnswerByQuestionId.get(item.getQuestionId())
                    : null;
            if (draft != null) {
                String originalSelected = examQuestionMapper.extractAnswerKey(draft.getUserAnswer());
                String displayedSelected = examQuestionMapper.resolveDisplayKeyFromOriginal(
                        originalSelected,
                        item.getOptionMap()
                );
                draftResponses.add(ModuleTestDraftAnswerResponse.builder()
                        .orderNo(item.getOrderNo())
                        .questionId(item.getQuestionId())
                        .selectedOption(displayedSelected)
                        .isFlagged(Boolean.TRUE.equals(draft.getIsFlagged()))
                        .build());
            }
        }

        Part part = module.getPart();
        ESection section = part != null && part.getSections() != null ? part.getSections() : ESection.READING;

        return ModuleTestRoomResponse.builder()
                .attemptId(attempt.getId())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .partName(part != null ? part.getName() : "Bài thi vượt ải")
                .section(section.name())
                .topic(part != null ? part.getName() : module.getTitle())
                .passScore(passScore)
                .targetScore(targetScore)
                .durationMinutes(resolveDurationMinutes(module))
                .startedAt(attempt.getStartedAt())
                .remainingSeconds(Math.max(0L, remainingSeconds))
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .draftAnswers(draftResponses)
                .build();
    }

    /**
     * Chuyển đổi một câu hỏi theo snapshot sang DTO phòng thi (ẩn hoàn toàn đáp án đúng và giải thích).
     */
    public ModuleTestRoomQuestionResponse toRoomQuestion(
            ModuleTestQuestionSnapshotItem snapshotItem,
            Question detailedQuestion
    ) {
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(
                detailedQuestion,
                snapshotItem.getOptionMap()
        );

        return ModuleTestRoomQuestionResponse.builder()
                .moduleTestQuestionId(snapshotItem.getModuleTestQuestionId())
                .questionId(content.getQuestionId())
                .orderNo(snapshotItem.getOrderNo())
                .parentId(content.getParentId())
                .partId(content.getPartId())
                .partNo(content.getPartNo())
                .partName(content.getPartName())
                .section(content.getSection())
                .title(content.getTitle())
                .instruction(content.getInstruction())
                .passage(content.getPassage())
                .audioUrl(content.getAudioUrl())
                .imageUrl(content.getImageUrl())
                .isGroupQuestion(content.getIsGroupQuestion())
                .options(content.getOptions())
                .build();
    }

    /**
     * Ánh xạ kết quả chấm bài ngay sau khi nộp bài thi vượt ải.
     */
    public SubmitModuleTestResponse toSubmitResponse(
            ModuleTestAttempt attempt,
            int passScore,
            int correctCount,
            int totalQuestions,
            Long nextModuleId,
            boolean isRoadmapCompleted
    ) {
        return SubmitModuleTestResponse.builder()
                .attemptId(attempt.getId())
                .moduleId(attempt.getModule().getId())
                .score(attempt.getScore() != null ? attempt.getScore() : 0)
                .passScore(passScore)
                .isPassed(attempt.getResult() == ETestResult.PASS)
                .correctCount(correctCount)
                .totalQuestions(totalQuestions)
                .nextModuleId(nextModuleId)
                .isRoadmapCompleted(isRoadmapCompleted)
                .build();
    }

    /**
     * Ánh xạ toàn bộ thông tin Báo cáo kết quả và Lịch sử thi vượt ải mô-đun (UC14).
     */
    public ModuleTestPerformanceReportResponse toPerformanceReport(
            Module module,
            ModuleTestAttempt currentAttempt,
            List<ModuleTestAttempt> submittedAttemptsDesc,
            int passScore,
            int correctAnswers,
            int totalQuestions,
            Long nextModuleId,
            boolean isRoadmapCompleted
    ) {
        int score = currentAttempt.getScore() != null ? currentAttempt.getScore() : 0;
        boolean isPassed = currentAttempt.getResult() == ETestResult.PASS;
        int diffFromPass = score - passScore;
        int accuracyRate = totalQuestions > 0
                ? (int) Math.round((correctAnswers * 100.0) / totalQuestions)
                : score;

        int durationSeconds = resolveAttemptDurationSeconds(currentAttempt, module);
        String durationFormatted = formatDuration(durationSeconds);
        int allowedMinutes = resolveDurationMinutes(module);
        String speedComparison = "Hoàn thành trong " + durationFormatted + " / " + allowedMinutes + " phút quy định";

        int gapResolvedPercent = passScore > 0
                ? Math.min(MAX_SCORE, (int) Math.round((score * 100.0) / passScore))
                : score;

        AiFeedbackResponse aiFeedback = buildAiFeedback(module, score, passScore, isPassed, isRoadmapCompleted);

        List<AttemptHistoryItemResponse> history = buildHistoryItems(
                submittedAttemptsDesc,
                currentAttempt.getId(),
                module
        );

        return ModuleTestPerformanceReportResponse.builder()
                .attemptId(currentAttempt.getId())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .completedAt(formatDateTime(currentAttempt.getSubmittedAt()))
                .score(score)
                .maxScore(MAX_SCORE)
                .passScore(passScore)
                .isPassed(isPassed)
                .diffFromPass(diffFromPass)
                .aiFeedback(aiFeedback)
                .metrics(MetricSummaryResponse.builder()
                        .accuracyRate(accuracyRate)
                        .correctAnswers(correctAnswers)
                        .totalQuestions(totalQuestions)
                        .durationFormatted(durationFormatted)
                        .speedComparison(speedComparison)
                        .competencyStatus(isPassed ? "ĐẠT CHUẨN" : "CẦN CỦNG CỐ")
                        .gapResolvedPercent(gapResolvedPercent)
                        .build())
                .history(history)
                .nextModuleId(nextModuleId)
                .isRoadmapCompleted(isRoadmapCompleted)
                .build();
    }

    /**
     * Ánh xạ toàn bộ dữ liệu Xem lại bài thi vượt ải đã nộp (UC14 Luồng 6a).
     */
    public ModuleTestReviewResponse toReviewResponse(
            Module module,
            ModuleTestAttempt attempt,
            int passScore,
            List<ModuleTestQuestionSnapshotItem> snapshotItems,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, ModuleTestAnswer> answerByQuestionId
    ) {
        List<ModuleTestQuestionSnapshotItem> safeSnapshot = snapshotItems != null
                ? snapshotItems.stream()
                        .sorted(Comparator.comparing(ModuleTestQuestionSnapshotItem::getOrderNo))
                        .toList()
                : Collections.emptyList();

        List<ModuleTestReviewQuestionResponse> reviewQuestions = new ArrayList<>(safeSnapshot.size());
        int correctCount = 0;

        for (ModuleTestQuestionSnapshotItem item : safeSnapshot) {
            Question detailed = detailedQuestionMap.get(item.getQuestionId());
            if (detailed == null) {
                continue;
            }
            ModuleTestAnswer answer = answerByQuestionId != null
                    ? answerByQuestionId.get(item.getQuestionId())
                    : null;
            ModuleTestReviewQuestionResponse mapped = toReviewQuestion(item, detailed, answer);
            if (Boolean.TRUE.equals(mapped.getIsCorrect())) {
                correctCount++;
            }
            reviewQuestions.add(mapped);
        }

        return ModuleTestReviewResponse.builder()
                .attemptId(attempt.getId())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .testTitle("Bài kiểm tra vượt ải — " + module.getTitle())
                .completedAt(attempt.getSubmittedAt())
                .score(attempt.getScore() != null ? attempt.getScore() : 0)
                .passScore(passScore)
                .isPassed(attempt.getResult() == ETestResult.PASS)
                .totalQuestions(reviewQuestions.size())
                .correctCount(correctCount)
                .questions(reviewQuestions)
                .build();
    }

    /**
     * Ánh xạ chi tiết một câu hỏi trong chế độ Xem lại bài thi vượt ải,
     * quy đổi đáp án đúng và đáp án học viên chọn về đúng thứ tự hiển thị (nếu lượt thi có shuffle A-B-C-D).
     */
    public ModuleTestReviewQuestionResponse toReviewQuestion(
            ModuleTestQuestionSnapshotItem snapshotItem,
            Question detailedQuestion,
            ModuleTestAnswer answer
    ) {
        Map<String, String> optionMap = snapshotItem.getOptionMap();
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(detailedQuestion, optionMap);

        String originalCorrectKey = examQuestionMapper.extractAnswerKey(detailedQuestion.getCorrectAnswer());
        String displayedCorrectKey = examQuestionMapper.resolveDisplayKeyFromOriginal(originalCorrectKey, optionMap);

        String originalUserKey = answer != null ? examQuestionMapper.extractAnswerKey(answer.getUserAnswer()) : null;
        String displayedUserKey = examQuestionMapper.resolveDisplayKeyFromOriginal(originalUserKey, optionMap);

        boolean isCorrect = answer != null && Boolean.TRUE.equals(answer.getIsCorrect());
        boolean isFlagged = answer != null && Boolean.TRUE.equals(answer.getIsFlagged());

        return ModuleTestReviewQuestionResponse.builder()
                .questionId(content.getQuestionId())
                .orderNo(snapshotItem.getOrderNo())
                .parentId(content.getParentId())
                .partId(content.getPartId())
                .partNo(content.getPartNo())
                .partName(content.getPartName())
                .section(content.getSection())
                .title(content.getTitle())
                .instruction(content.getInstruction())
                .passage(content.getPassage())
                .audioUrl(content.getAudioUrl())
                .imageUrl(content.getImageUrl())
                .isGroupQuestion(content.getIsGroupQuestion())
                .options(content.getOptions())
                .userAnswer(displayedUserKey)
                .correctAnswer(displayedCorrectKey)
                .isCorrect(isCorrect)
                .isFlagged(isFlagged)
                .explanation(examQuestionMapper.resolveExplanation(detailedQuestion))
                .build();
    }

    private AiFeedbackResponse buildAiFeedback(
            Module module,
            int score,
            int passScore,
            boolean isPassed,
            boolean isRoadmapCompleted
    ) {
        if (isPassed) {
            if (isRoadmapCompleted) {
                return AiFeedbackResponse.builder()
                        .title("Xuất sắc! Bạn đã hoàn thành toàn bộ lộ trình học")
                        .content("Bạn đạt " + score + "/100 điểm (vượt ngưỡng " + passScore
                                + ") ở mô-đun \"" + module.getTitle()
                                + "\". Toàn bộ các chặng trên lộ trình đã hoàn tất và bài thi thử tổng hợp 200 câu đã sẵn sàng.")
                        .build();
            }
            return AiFeedbackResponse.builder()
                    .title("Chúc mừng! Bạn đã vượt qua ngưỡng chuẩn của mô-đun")
                    .content("Với số điểm " + score + "/100 (ngưỡng yêu cầu " + passScore
                            + "), năng lực chuyên đề \"" + module.getTitle()
                            + "\" đã được xác nhận đạt chuẩn. Mô-đun kế tiếp trên lộ trình của bạn đã được mở khóa.")
                    .build();
        }

        int missing = Math.max(1, passScore - score);
        return AiFeedbackResponse.builder()
                .title("Chưa đạt điểm đỗ — Cần củng cố thêm " + missing + " điểm")
                .content("Bạn đạt " + score + "/100 điểm, còn thiếu " + missing
                        + " điểm so với ngưỡng chuẩn (" + passScore + "/100) của mục tiêu hiện tại. "
                        + "Hãy bấm \"Xem lại bài thi\" để phân tích kỹ các câu sai và thi lại bất cứ lúc nào.")
                .build();
    }

    private List<AttemptHistoryItemResponse> buildHistoryItems(
            List<ModuleTestAttempt> submittedAttemptsDesc,
            Long currentAttemptId,
            Module module
    ) {
        if (submittedAttemptsDesc == null || submittedAttemptsDesc.isEmpty()) {
            return Collections.emptyList();
        }
        int totalAttempts = submittedAttemptsDesc.size();
        List<AttemptHistoryItemResponse> items = new ArrayList<>(totalAttempts);
        for (int i = 0; i < totalAttempts; i++) {
            ModuleTestAttempt att = submittedAttemptsDesc.get(i);
            int attemptNumber = totalAttempts - i;
            int attemptScore = att.getScore() != null ? att.getScore() : 0;
            int attemptDuration = resolveAttemptDurationSeconds(att, module);
            items.add(AttemptHistoryItemResponse.builder()
                    .attemptNumber(attemptNumber)
                    .attemptId(att.getId())
                    .isLatest(att.getId().equals(currentAttemptId))
                    .completedAt(formatDateTime(att.getSubmittedAt()))
                    .score(attemptScore)
                    .maxScore(MAX_SCORE)
                    .isPassed(att.getResult() == ETestResult.PASS)
                    .durationFormatted(formatDuration(attemptDuration))
                    .build());
        }
        return items;
    }

    private int resolveDurationMinutes(Module module) {
        return module.getTestDurationMinutes() != null && module.getTestDurationMinutes() > 0
                ? module.getTestDurationMinutes()
                : DEFAULT_DURATION_MINUTES;
    }

    private int resolveAttemptDurationSeconds(ModuleTestAttempt attempt, Module module) {
        if (attempt.getDurationSeconds() != null && attempt.getDurationSeconds() >= 0) {
            return attempt.getDurationSeconds();
        }
        return resolveDurationMinutes(module) * 60;
    }

    private String formatDuration(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int minutes = safe / 60;
        int seconds = safe % 60;
        if (minutes == 0) {
            return seconds + " giây";
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(REPORT_DATE_FORMATTER) : "";
    }
}
