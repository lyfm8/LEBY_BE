package com.example.be.mapper.mockexam;

import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.dto.response.mockexam.MockExamDraftAnswerResponse;
import com.example.be.dto.response.mockexam.MockExamHistoryItemResponse;
import com.example.be.dto.response.mockexam.MockExamListItemResponse;
import com.example.be.dto.response.mockexam.MockExamListResponse;
import com.example.be.dto.response.mockexam.MockExamReviewResponse;
import com.example.be.dto.response.mockexam.MockExamRoomResponse;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.mapper.diagnostic.DiagnosticReviewMapper;
import com.example.be.mapper.diagnostic.DiagnosticRoomMapper;
import com.example.be.mapper.question.ExamQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyên biệt chuyển đổi dữ liệu Danh sách đề, Phòng thi, Nháp đáp án và Xem lại bài thi
 * cho phân hệ Thi thử TOEIC chuẩn ETS 200 câu (UC17).
 * Tái sử dụng {@link DiagnosticRoomMapper}, {@link DiagnosticReviewMapper} và {@link ExamQuestionMapper}.
 */
@Component
@RequiredArgsConstructor
public class MockExamRoomMapper {

    private final DiagnosticRoomMapper diagnosticRoomMapper;
    private final DiagnosticReviewMapper diagnosticReviewMapper;
    private final ExamQuestionMapper examQuestionMapper;

    /**
     * Ánh xạ thông tin một đề thi thử sang DTO hiển thị trên danh sách phòng thi (UC17 Bước 2).
     */
    public MockExamListItemResponse toListItemResponse(
            DiagnosticTest test,
            int totalQuestions,
            boolean isLocked,
            String lockReason,
            DiagnosticAttempt inProgressAttempt,
            int attemptCount,
            Integer bestScore
    ) {
        return MockExamListItemResponse.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .examTag(test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE)
                .durationMinutes(test.getDurationMinutes())
                .totalQuestions(totalQuestions)
                .isLocked(isLocked)
                .lockReason(isLocked ? lockReason : null)
                .hasInProgressAttempt(inProgressAttempt != null)
                .inProgressAttemptId(inProgressAttempt != null ? inProgressAttempt.getId() : null)
                .attemptCount(attemptCount)
                .bestScore(bestScore)
                .build();
    }

    /**
     * Ánh xạ một lượt thi thử đã hoàn thành sang dòng lịch sử thi (UC17 & UC18).
     */
    public MockExamHistoryItemResponse toHistoryItemResponse(
            DiagnosticAttempt attempt,
            int listeningScore,
            int readingScore,
            int totalCorrect,
            int totalQuestions,
            Integer targetScore
    ) {
        DiagnosticTest test = attempt.getDiagnosticTest();
        EExamTag examTag = test != null && test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE;
        int totalToeicScore = listeningScore + readingScore;
        Boolean isAimAchieved = (examTag == EExamTag.OUTPUT && targetScore != null)
                ? totalToeicScore >= targetScore
                : null;
        boolean canViewReview = MockExamReportMapper.isReviewUnlocked(examTag, totalToeicScore, targetScore);

        return MockExamHistoryItemResponse.builder()
                .attemptId(attempt.getId())
                .testId(test != null ? test.getId() : null)
                .testTitle(test != null ? test.getTitle() : "Đề thi thử TOEIC")
                .examTag(examTag)
                .status(attempt.getStatus())
                .completedAt(attempt.getCompletedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .listeningScore(listeningScore)
                .readingScore(readingScore)
                .totalToeicScore(totalToeicScore)
                .totalCorrect(totalCorrect)
                .totalQuestions(totalQuestions)
                .isAimAchieved(isAimAchieved)
                .canViewReview(canViewReview)
                .build();
    }

    /**
     * Ánh xạ tổng thể dữ liệu danh mục đề thi thử, trạng thái mở khóa đề đầu ra và phiên đang dở.
     */
    public MockExamListResponse toListResponse(
            boolean isOutputUnlocked,
            String outputLockReason,
            DiagnosticAttempt activeAttempt,
            Integer userTargetScore,
            List<MockExamListItemResponse> exams,
            List<MockExamHistoryItemResponse> history
    ) {
        DiagnosticTest activeTest = activeAttempt != null ? activeAttempt.getDiagnosticTest() : null;
        return MockExamListResponse.builder()
                .isOutputExamUnlocked(isOutputUnlocked)
                .outputLockReason(isOutputUnlocked ? null : outputLockReason)
                .activeAttemptId(activeAttempt != null ? activeAttempt.getId() : null)
                .activeTestId(activeTest != null ? activeTest.getId() : null)
                .activeTestTitle(activeTest != null ? activeTest.getTitle() : null)
                .userTargetScore(userTargetScore)
                .exams(exams != null ? exams : Collections.emptyList())
                .history(history != null ? history : Collections.emptyList())
                .build();
    }

    /**
     * Ánh xạ thông tin lượt thi thử vừa khởi tạo sang {@link StartDiagnosticResponse}.
     */
    public StartDiagnosticResponse toStartResponse(
            DiagnosticAttempt attempt,
            DiagnosticTest test,
            Long targetProfileId
    ) {
        return diagnosticRoomMapper.toStartResponse(attempt, test, targetProfileId);
    }

    /**
     * Ánh xạ toàn bộ 200 câu hỏi, thời gian đếm ngược và bản nháp máy chủ sang {@link MockExamRoomResponse}.
     * Tuyệt đối không trả ra đáp án đúng hay giải thích trong lúc làm bài.
     */
    public MockExamRoomResponse toRoomResponse(
            DiagnosticTest test,
            DiagnosticAttempt attempt,
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, DiagnosticAnswer> draftByQuestionId,
            long remainingSeconds
    ) {
        List<DiagnosticTestQuestion> safeOrdered = orderedQuestions != null
                ? orderedQuestions.stream().sorted(Comparator.comparing(DiagnosticTestQuestion::getOrderNo)).toList()
                : Collections.emptyList();

        List<DiagnosticQuestionResponse> questionResponses = new ArrayList<>(safeOrdered.size());
        List<MockExamDraftAnswerResponse> draftAnswers = new ArrayList<>();

        for (DiagnosticTestQuestion tq : safeOrdered) {
            Long questionId = tq.getQuestion().getId();
            Question detailed = detailedQuestionMap != null
                    ? detailedQuestionMap.getOrDefault(questionId, tq.getQuestion())
                    : tq.getQuestion();
            questionResponses.add(diagnosticRoomMapper.toQuestionResponse(tq, detailed));

            DiagnosticAnswer draft = draftByQuestionId != null ? draftByQuestionId.get(questionId) : null;
            if (draft != null) {
                String selectedKey = examQuestionMapper.extractAnswerKey(draft.getUserAnswer());
                boolean isFlagged = Boolean.TRUE.equals(draft.getIsFlagged());
                if (selectedKey != null || isFlagged) {
                    draftAnswers.add(MockExamDraftAnswerResponse.builder()
                            .questionId(questionId)
                            .orderNo(tq.getOrderNo())
                            .selectedOption(selectedKey)
                            .isFlagged(isFlagged)
                            .build());
                }
            }
        }

        Long targetProfileId = attempt.getTargetProfile() != null
                ? attempt.getTargetProfile().getId()
                : (attempt.getUser() != null && attempt.getUser().getTargetProfile() != null
                        ? attempt.getUser().getTargetProfile().getId()
                        : null);

        return MockExamRoomResponse.builder()
                .attemptId(attempt.getId())
                .testId(test.getId())
                .targetProfileId(targetProfileId)
                .title(test.getTitle())
                .description(test.getDescription())
                .examTag(test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE)
                .durationMinutes(test.getDurationMinutes())
                .startedAt(attempt.getStartedAt())
                .remainingSeconds(Math.max(0L, remainingSeconds))
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .draftAnswers(draftAnswers)
                .build();
    }

    /**
     * Ánh xạ dữ liệu lượt thi thử đã hoàn thành sang DTO phòng xem lại kèm đáp án đúng và giải thích.
     */
    public MockExamReviewResponse toReviewResponse(
            DiagnosticTest test,
            DiagnosticAttempt attempt,
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            Map<Long, DiagnosticAnswer> answerByQuestionId
    ) {
        DiagnosticReviewResponse base = diagnosticReviewMapper.toReviewResponse(
                test,
                attempt,
                orderedQuestions,
                detailedQuestionMap,
                answerByQuestionId
        );

        return MockExamReviewResponse.builder()
                .attemptId(base.getAttemptId())
                .testId(base.getTestId())
                .testTitle(base.getTestTitle())
                .examTag(test != null && test.getExamTag() != null ? test.getExamTag() : EExamTag.PRACTICE)
                .completedAt(base.getCompletedAt())
                .totalQuestions(base.getTotalQuestions())
                .correctCount(base.getCorrectCount())
                .questions(base.getQuestions())
                .build();
    }
}
