package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticOptionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.dto.response.diagnostic.StartDiagnosticResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.mapper.question.ExamQuestionMapper.MappedQuestionContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mapper chuyển đổi dữ liệu Đề thi và Câu hỏi sang các DTO phục vụ Phòng thi chẩn đoán (UC06).
 * Tái sử dụng {@link ExamQuestionMapper} để parse ngữ cảnh câu hỏi, passage và media;
 * tuyệt đối không trả ra trường đáp án đúng hay lời giải chi tiết trong quá trình thi.
 */
@Component
public class DiagnosticRoomMapper {

    private final ExamQuestionMapper examQuestionMapper;

    @Autowired
    public DiagnosticRoomMapper(ExamQuestionMapper examQuestionMapper) {
        this.examQuestionMapper = examQuestionMapper;
    }

    /**
     * Constructor dự phòng giữ tương thích cho các unit test khởi tạo trực tiếp với {@link ObjectMapper}.
     */
    public DiagnosticRoomMapper(ObjectMapper objectMapper) {
        this(new ExamQuestionMapper(objectMapper));
    }

    /**
     * Ánh xạ thông tin lượt làm bài chẩn đoán vừa khởi tạo hoặc khôi phục sang {@link StartDiagnosticResponse}.
     */
    public StartDiagnosticResponse toStartResponse(
            DiagnosticAttempt attempt,
            DiagnosticTest test,
            Long targetProfileId
    ) {
        return StartDiagnosticResponse.builder()
                .attemptId(attempt.getId())
                .diagnosticTestId(test.getId())
                .targetProfileId(targetProfileId)
                .startedAt(attempt.getStartedAt())
                .durationMinutes(test.getDurationMinutes())
                .build();
    }

    /**
     * Ánh xạ lượt làm bài chẩn đoán và danh sách câu hỏi đã nạp chi tiết sang DTO phòng thi.
     */
    public DiagnosticRoomResponse toRoomResponse(
            DiagnosticTest test,
            DiagnosticAttempt attempt,
            List<DiagnosticTestQuestion> orderedQuestions,
            Map<Long, Question> detailedQuestionMap,
            long remainingSeconds
    ) {
        List<DiagnosticTestQuestion> safeOrdered = orderedQuestions != null
                ? orderedQuestions.stream().sorted(Comparator.comparing(DiagnosticTestQuestion::getOrderNo)).toList()
                : Collections.emptyList();

        List<DiagnosticQuestionResponse> questionResponses = new ArrayList<>(safeOrdered.size());
        for (DiagnosticTestQuestion tq : safeOrdered) {
            Question detailed = detailedQuestionMap != null
                    ? detailedQuestionMap.getOrDefault(tq.getQuestion().getId(), tq.getQuestion())
                    : tq.getQuestion();
            questionResponses.add(toQuestionResponse(tq, detailed));
        }

        Long targetProfileId = attempt.getTargetProfile() != null
                ? attempt.getTargetProfile().getId()
                : (attempt.getUser() != null && attempt.getUser().getTargetProfile() != null
                        ? attempt.getUser().getTargetProfile().getId()
                        : null);

        return DiagnosticRoomResponse.builder()
                .attemptId(attempt.getId())
                .testId(test.getId())
                .targetProfileId(targetProfileId)
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .startedAt(attempt.getStartedAt())
                .remainingSeconds(Math.max(0L, remainingSeconds))
                .totalQuestions(questionResponses.size())
                .questions(questionResponses)
                .build();
    }

    /**
     * Ánh xạ một mục câu hỏi trong đề thi sang DTO hiển thị trong phòng thi chẩn đoán.
     */
    public DiagnosticQuestionResponse toQuestionResponse(DiagnosticTestQuestion testQuestion, Question question) {
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(question, null);
        List<DiagnosticOptionResponse> options = content.getOptions().stream()
                .map(opt -> DiagnosticOptionResponse.builder()
                        .key(opt.getKey())
                        .text(opt.getText())
                        .build())
                .toList();

        return DiagnosticQuestionResponse.builder()
                .questionId(content.getQuestionId())
                .orderNo(testQuestion.getOrderNo())
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
                .options(options)
                .build();
    }

    /**
     * Suy luận số thứ tự Part (1..7) từ tên Part hoặc ID của Part.
     */
    public int resolvePartNumber(Part part) {
        return examQuestionMapper.resolvePartNumber(part);
    }
}
