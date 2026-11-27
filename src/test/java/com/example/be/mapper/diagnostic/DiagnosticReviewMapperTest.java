package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticReviewQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticReviewResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EAttemptStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link DiagnosticReviewMapper} phục vụ chế độ Xem lại bài thi chẩn đoán (UC07 - BR14).
 */
class DiagnosticReviewMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DiagnosticRoomMapper roomMapper = new DiagnosticRoomMapper(objectMapper);
    private final DiagnosticReviewMapper reviewMapper = new DiagnosticReviewMapper(roomMapper, objectMapper);

    @Test
    void toReviewResponse_mapsCorrectWrongSkippedAndParentExplanationFallback() {
        Part part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1: Photographs");
        part1.setSections(ESection.LISTENING);

        Part part3 = new Part();
        part3.setId(3L);
        part3.setName("Part 3: Conversations");
        part3.setSections(ESection.LISTENING);

        // Câu 1: Học viên chọn đúng, có đầy đủ giải thích và vocabulary array
        Question q1 = new Question();
        q1.setId(1L);
        q1.setPart(part1);
        q1.setSections(ESection.LISTENING);
        q1.setQuestionData("{\"text\":\"Look at photo\",\"audioUrl\":\"https://cdn/p1.mp3\",\"options\":[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"}]}");
        q1.setCorrectAnswer("{\"key\":\"A\"}");
        q1.setExplanation("{\"transcript\":\"(A) Inspecting\",\"translation\":\"Đang kiểm tra\",\"clue\":\"Từ khóa inspecting\",\"analysis\":\"Loại B\",\"vocabulary\":[{\"word\":\"inspect\",\"type\":\"v\",\"meaning\":\"kiểm tra\"}]}");

        // Câu 2: Câu con Part 3 chọn sai, thiếu transcript/translation riêng nên fallback từ cha
        Question p3Parent = new Question();
        p3Parent.setId(10L);
        p3Parent.setPart(part3);
        p3Parent.setSections(ESection.LISTENING);
        p3Parent.setQuestionData("{\"instruction\":\"Listen to conversation\",\"audioUrl\":\"https://cdn/p3.mp3\"}");
        p3Parent.setExplanation("{\"transcript\":\"W: Hello\\nM: Hi\",\"translation\":\"Nữ: Xin chào\\nNam: Chào\"}");

        Question q2 = new Question();
        q2.setId(2L);
        q2.setPart(part3);
        q2.setSections(ESection.LISTENING);
        q2.setParent(p3Parent);
        q2.setQuestionData("{\"text\":\"Where are they?\",\"options\":[{\"key\":\"A\",\"text\":\"Office\"},{\"key\":\"B\",\"text\":\"Cafe\"}]}");
        q2.setCorrectAnswer("{\"selected\":\"B\",\"key\":\"B\"}");
        q2.setExplanation("{\"clue\":\"Nghe thấy cafe\",\"analysis\":\"Đáp án B đúng\"}");

        // Câu 3: Bỏ qua không chọn đáp án và không có giải thích
        Question q3 = new Question();
        q3.setId(3L);
        q3.setPart(part1);
        q3.setSections(ESection.LISTENING);
        q3.setQuestionData("{\"text\":\"Skipped question\",\"options\":[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]}");
        q3.setCorrectAnswer("{\"key\":\"A\"}");
        q3.setExplanation(null);

        DiagnosticTestQuestion link1 = new DiagnosticTestQuestion();
        link1.setOrderNo(1);
        link1.setQuestion(q1);

        DiagnosticTestQuestion link2 = new DiagnosticTestQuestion();
        link2.setOrderNo(2);
        link2.setQuestion(q2);

        DiagnosticTestQuestion link3 = new DiagnosticTestQuestion();
        link3.setOrderNo(3);
        link3.setQuestion(q3);

        DiagnosticAnswer ans1 = new DiagnosticAnswer();
        ans1.setQuestion(q1);
        ans1.setUserAnswer("{\"selected\":\"a\"}");
        ans1.setIsCorrect(true);
        ans1.setIsFlagged(true);

        DiagnosticAnswer ans2 = new DiagnosticAnswer();
        ans2.setQuestion(q2);
        ans2.setUserAnswer("{\"selected\":\"A\"}");
        ans2.setIsCorrect(false);
        ans2.setIsFlagged(false);

        DiagnosticTest test = new DiagnosticTest();
        test.setId(5L);
        test.setTitle("Diagnostic 48");

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(100L);
        attempt.setDiagnosticTest(test);
        attempt.setStatus(EAttemptStatus.SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.of(2026, 11, 27, 9, 30));

        DiagnosticReviewResponse response = reviewMapper.toReviewResponse(
                test,
                attempt,
                List.of(link2, link3, link1),
                Map.of(1L, q1, 2L, q2, 3L, q3),
                Map.of(1L, ans1, 2L, ans2)
        );

        assertThat(response.getAttemptId()).isEqualTo(100L);
        assertThat(response.getTotalQuestions()).isEqualTo(3);
        assertThat(response.getCorrectCount()).isEqualTo(1);
        assertThat(response.getQuestions()).hasSize(3);

        DiagnosticReviewQuestionResponse r1 = response.getQuestions().get(0);
        assertThat(r1.getOrderNo()).isEqualTo(1);
        assertThat(r1.getUserAnswer()).isEqualTo("A");
        assertThat(r1.getCorrectAnswer()).isEqualTo("A");
        assertThat(r1.getIsCorrect()).isTrue();
        assertThat(r1.getIsFlagged()).isTrue();
        assertThat(r1.getExplanation()).isNotNull();
        assertThat(r1.getExplanation().getVocabulary()).hasSize(1);
        assertThat(r1.getExplanation().getVocabulary().get(0).getWord()).isEqualTo("inspect");

        DiagnosticReviewQuestionResponse r2 = response.getQuestions().get(1);
        assertThat(r2.getOrderNo()).isEqualTo(2);
        assertThat(r2.getUserAnswer()).isEqualTo("A");
        assertThat(r2.getCorrectAnswer()).isEqualTo("B");
        assertThat(r2.getIsCorrect()).isFalse();
        assertThat(r2.getAudioUrl()).isEqualTo("https://cdn/p3.mp3");
        assertThat(r2.getExplanation()).isNotNull();
        assertThat(r2.getExplanation().getTranscript()).isEqualTo("W: Hello\nM: Hi");
        assertThat(r2.getExplanation().getTranslation()).isEqualTo("Nữ: Xin chào\nNam: Chào");
        assertThat(r2.getExplanation().getClue()).isEqualTo("Nghe thấy cafe");

        DiagnosticReviewQuestionResponse r3 = response.getQuestions().get(2);
        assertThat(r3.getOrderNo()).isEqualTo(3);
        assertThat(r3.getUserAnswer()).isNull();
        assertThat(r3.getCorrectAnswer()).isEqualTo("A");
        assertThat(r3.getIsCorrect()).isFalse();
        assertThat(r3.getExplanation()).isNull();
    }
}
