package com.example.be.mapper.content;

import com.example.be.dto.response.content.MistakeQuestionItemResponse;
import com.example.be.dto.response.content.MistakeQuestionItemResponse.AbilityTag;
import com.example.be.dto.response.content.MistakeRetrySetResponse.RetryQuestionItem;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse.QuestionRetryResult;
import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.entity.question.Question;
import com.example.be.enums.content.EMistakeSourceType;
import com.example.be.mapper.question.ExamQuestionMapper;
import com.example.be.mapper.question.ExamQuestionMapper.MappedQuestionContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Mapper chuyên biệt chuyển đổi câu hỏi làm sai và kết quả làm lại sang các DTO của Sổ tay câu hỏi làm sai (UC15).
 * Tái sử dụng trực tiếp {@link ExamQuestionMapper} để đồng bộ cách bóc tách ngữ liệu chùm, media và lời giải BR14.
 */
@Component
@RequiredArgsConstructor
public class UserMistakeNotebookMapper {

    private static final String LABEL_DIAGNOSTIC = "Bài thi chẩn đoán";
    private static final String LABEL_MODULE_TEST = "Bài thi vượt ải";
    private static final String LABEL_FULL_MOCK_TEST = "Bài thi thử TOEIC";

    private final ExamQuestionMapper examQuestionMapper;

    /**
     * Chuyển đổi một câu hỏi làm sai sang DTO đầy đủ phục vụ chế độ Xem lại trong Sổ tay (kèm đáp án và lời giải BR14).
     *
     * @param question       Thực thể câu hỏi đã nạp sẵn Part, Parent và QuestionMedias qua QuestionBatchLoader
     * @param abilities      Danh sách kỹ năng (Ability) gắn với câu hỏi
     * @param sourceType     Nguồn bài thi phát sinh lỗi sai gần nhất
     * @param sourceLabel    Tên bài thi / mô-đun phát sinh lỗi sai gần nhất
     * @param wrongCount     Tổng số lần học viên làm sai câu hỏi này
     * @param lastWrongAt    Thời điểm làm sai gần nhất
     * @param lastUserAnswer Ký hiệu đáp án (A/B/C/D) học viên chọn sai gần nhất (đã chuẩn hóa)
     * @return {@link MistakeQuestionItemResponse}
     */
    public MistakeQuestionItemResponse toMistakeItemResponse(
            Question question,
            List<AbilityTag> abilities,
            EMistakeSourceType sourceType,
            String sourceLabel,
            int wrongCount,
            LocalDateTime lastWrongAt,
            String lastUserAnswer
    ) {
        // Bóc tách nội dung câu hỏi, ngữ cảnh câu cha (Part 3, 4, 6, 7), media và các phương án gốc không xáo trộn
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(question, null);
        // Lấy đáp án đúng chuẩn hóa (A/B/C/D) và hợp nhất lời giải BR14 (kế thừa transcript/translation từ câu cha nếu có)
        String correctAnswer = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
        QuestionExplanationResponse explanation = examQuestionMapper.resolveExplanation(question);

        return MistakeQuestionItemResponse.builder()
                .questionId(content.getQuestionId())
                .parentId(content.getParentId())
                .partId(content.getPartId())
                .partNo(content.getPartNo())
                .partName(content.getPartName())
                .section(content.getSection())
                .abilities(abilities != null ? abilities : Collections.emptyList())
                .sourceType(sourceType)
                .sourceLabel(resolveSourceLabel(sourceType, sourceLabel))
                .wrongCount(wrongCount)
                .lastWrongAt(lastWrongAt)
                .title(content.getTitle())
                .instruction(content.getInstruction())
                .passage(content.getPassage())
                .audioUrl(content.getAudioUrl())
                .imageUrl(content.getImageUrl())
                .isGroupQuestion(content.getIsGroupQuestion())
                .options(content.getOptions())
                .lastUserAnswer(lastUserAnswer)
                .correctAnswer(correctAnswer)
                .explanation(explanation)
                .build();
    }

    /**
     * Chuyển đổi một câu hỏi sai sang DTO phục vụ phiên Làm lại (UC15 Luồng 5a).
     * SECURITY: Cố ý không ánh xạ {@code correctAnswer} và {@code explanation} để chống lộ đáp án trước khi nộp bài.
     *
     * @param orderNo     Số thứ tự hiển thị trong phiên làm lại (1..N)
     * @param question    Thực thể câu hỏi đã nạp đầy đủ Part, Parent và Media
     * @param abilities   Danh sách kỹ năng gắn với câu hỏi
     * @param sourceType  Nguồn phát sinh lỗi sai gần nhất
     * @param sourceLabel Tên bài thi / mô-đun nguồn
     * @param wrongCount  Số lần từng làm sai
     * @return {@link RetryQuestionItem}
     */
    public RetryQuestionItem toRetryQuestionItem(
            int orderNo,
            Question question,
            List<AbilityTag> abilities,
            EMistakeSourceType sourceType,
            String sourceLabel,
            int wrongCount
    ) {
        // Chỉ trích xuất đề bài, ngữ cảnh đoạn văn, ảnh/audio và danh sách lựa chọn A-B-C-D
        MappedQuestionContent content = examQuestionMapper.extractQuestionContent(question, null);

        return RetryQuestionItem.builder()
                .questionId(content.getQuestionId())
                .orderNo(orderNo)
                .parentId(content.getParentId())
                .partId(content.getPartId())
                .partNo(content.getPartNo())
                .partName(content.getPartName())
                .section(content.getSection())
                .abilities(abilities != null ? abilities : Collections.emptyList())
                .sourceType(sourceType)
                .sourceLabel(resolveSourceLabel(sourceType, sourceLabel))
                .wrongCount(wrongCount)
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
     * Tạo kết quả chấm điểm cho 1 câu trong phiên làm lại kèm lời giải chi tiết BR14.
     *
     * @param question       Thực thể câu hỏi (đã nạp kèm câu cha để lấy transcript/translation nếu có)
     * @param selectedAnswer Đáp án học viên nộp (có thể null nếu bỏ trống)
     * @return {@link QuestionRetryResult}
     */
    public QuestionRetryResult toQuestionRetryResult(Question question, String selectedAnswer) {
        // Chuẩn hóa đáp án học viên chọn và đáp án đúng từ ngân hàng câu hỏi về chữ in hoa (A/B/C/D)
        String normalizedSelected = examQuestionMapper.normalizeOptionKey(selectedAnswer);
        String correctAnswer = examQuestionMapper.extractAnswerKey(question.getCorrectAnswer());
        // Chỉ chấm đúng khi học viên có chọn đáp án và khớp chính xác với đáp án chuẩn
        boolean isCorrect = normalizedSelected != null
                && correctAnswer != null
                && normalizedSelected.equalsIgnoreCase(correctAnswer);
        // Hợp nhất lời giải chi tiết chuẩn BR14 (manh mối, phân tích, lời thoại, bản dịch, từ vựng)
        QuestionExplanationResponse explanation = examQuestionMapper.resolveExplanation(question);

        return QuestionRetryResult.builder()
                .questionId(question.getId())
                .selectedAnswer(normalizedSelected)
                .correctAnswer(correctAnswer)
                .isCorrect(isCorrect)
                .explanation(explanation)
                .build();
    }

    /**
     * Trích xuất đáp án chữ cái (A/B/C/D) từ chuỗi JSON {@code user_answer} lưu trong lịch sử thi.
     *
     * @param rawUserAnswer Chuỗi JSON hoặc ký tự đáp án thô từ DB
     * @return Chữ cái đáp án viết hoa hoặc {@code null} nếu học viên bỏ trống
     */
    public String extractAnswerKey(String rawUserAnswer) {
        return examQuestionMapper.extractAnswerKey(rawUserAnswer);
    }

    /**
     * Quy đổi mã nguồn lỗi sai sang nhãn tiếng Việt hiển thị thân thiện trên giao diện.
     *
     * @param sourceType Loại nguồn phát sinh lỗi sai
     * @return Nhãn tiếng Việt tương ứng
     */
    public String resolveSourceTypeDisplayLabel(EMistakeSourceType sourceType) {
        if (sourceType == null) {
            return LABEL_DIAGNOSTIC;
        }
        return switch (sourceType) {
            case DIAGNOSTIC -> LABEL_DIAGNOSTIC;
            case MODULE_TEST -> LABEL_MODULE_TEST;
            case FULL_MOCK_TEST -> LABEL_FULL_MOCK_TEST;
        };
    }

    private String resolveSourceLabel(EMistakeSourceType sourceType, String rawLabel) {
        if (rawLabel != null && !rawLabel.isBlank()) {
            return rawLabel.trim();
        }
        return resolveSourceTypeDisplayLabel(sourceType);
    }
}
