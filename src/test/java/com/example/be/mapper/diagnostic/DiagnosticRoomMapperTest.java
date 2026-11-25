package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticQuestionResponse;
import com.example.be.dto.response.diagnostic.DiagnosticRoomResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.enums.media.EMediaResourceType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link DiagnosticRoomMapper} phục vụ UC06.
 */
class DiagnosticRoomMapperTest {

    private final DiagnosticRoomMapper mapper = new DiagnosticRoomMapper(new ObjectMapper());

    @Test
    void toRoomResponse_mapsPart1Part3Part7WithoutLeakingPassageOrTranscriptForListening() {
        Part part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1: Photographs");
        part1.setSections(ESection.LISTENING);

        Part part3 = new Part();
        part3.setId(3L);
        part3.setName("Part 3: Conversations");
        part3.setSections(ESection.LISTENING);

        Part part7 = new Part();
        part7.setId(7L);
        part7.setName("Part 7: Reading Comprehension");
        part7.setSections(ESection.READING);

        // Part 1 standalone có cả ảnh và audio trong questionData JSON
        Question q1 = new Question();
        q1.setId(1L);
        q1.setPart(part1);
        q1.setSections(ESection.LISTENING);
        q1.setQuestionData("{\"text\":\"Look at the photo.\",\"imageUrl\":\"https://cdn/p1.jpg\",\"audioUrl\":\"https://cdn/p1.mp3\",\"options\":[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"}]}");
        q1.setCorrectAnswer("{\"key\":\"A\"}");

        // Part 3 parent có audio qua QuestionMedia + instruction ngắn; child không nên có passage
        Question p3Parent = new Question();
        p3Parent.setId(10L);
        p3Parent.setPart(part3);
        p3Parent.setSections(ESection.LISTENING);
        p3Parent.setQuestionData("{\"instruction\":\"Questions 2 through 4 refer to the following conversation.\",\"passage\":\"Transcript should not leak\"}");
        Media p3Audio = Media.builder()
                .secureUrl("https://cdn/p3_shared.mp3")
                .resourceType(EMediaResourceType.VIDEO)
                .format("mp3")
                .build();
        QuestionMedia p3Qm = QuestionMedia.builder().question(p3Parent).media(p3Audio).orderNo(1).build();
        p3Parent.setQuestionMedias(List.of(p3Qm));

        Question q2 = new Question();
        q2.setId(2L);
        q2.setPart(part3);
        q2.setSections(ESection.LISTENING);
        q2.setParent(p3Parent);
        q2.setQuestionData("{\"text\":\"What is discussed?\",\"options\":[{\"key\":\"A\",\"text\":\"Budget\"}]}");
        q2.setCorrectAnswer("{\"key\":\"A\"}");

        // Part 7 parent có passage dài; child phải nhận được passage từ parent
        Question p7Parent = new Question();
        p7Parent.setId(20L);
        p7Parent.setPart(part7);
        p7Parent.setSections(ESection.READING);
        p7Parent.setQuestionData("{\"instruction\":\"Read the notice.\",\"passage\":\"GLOBAL LOGISTICS NOTICE...\"}");

        Question q3 = new Question();
        q3.setId(3L);
        q3.setPart(part7);
        q3.setSections(ESection.READING);
        q3.setParent(p7Parent);
        q3.setQuestionData("{\"text\":\"What is required?\",\"options\":[{\"key\":\"A\",\"text\":\"8 years\"}]}");
        q3.setCorrectAnswer("{\"key\":\"A\"}");

        DiagnosticTestQuestion dtq1 = new DiagnosticTestQuestion();
        dtq1.setOrderNo(1);
        dtq1.setQuestion(q1);

        DiagnosticTestQuestion dtq2 = new DiagnosticTestQuestion();
        dtq2.setOrderNo(2);
        dtq2.setQuestion(q2);

        DiagnosticTestQuestion dtq3 = new DiagnosticTestQuestion();
        dtq3.setOrderNo(3);
        dtq3.setQuestion(q3);

        DiagnosticTest test = new DiagnosticTest();
        test.setId(5L);
        test.setTitle("Diagnostic 45P");
        test.setDurationMinutes(45);
        test.setTestType(ETestType.DIAGNOSTIC);

        TargetProfile target = new TargetProfile();
        target.setId(3L);

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(101L);
        attempt.setDiagnosticTest(test);
        attempt.setTargetProfile(target);
        attempt.setStatus(EAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(LocalDateTime.now());

        DiagnosticRoomResponse room = mapper.toRoomResponse(
                test,
                attempt,
                List.of(dtq3, dtq1, dtq2), // cố tình xáo thứ tự đầu vào để kiểm tra sort theo orderNo
                Map.of(1L, q1, 2L, q2, 3L, q3),
                2500L
        );

        assertThat(room.getAttemptId()).isEqualTo(101L);
        assertThat(room.getTargetProfileId()).isEqualTo(3L);
        assertThat(room.getRemainingSeconds()).isEqualTo(2500L);
        assertThat(room.getTotalQuestions()).isEqualTo(3);

        DiagnosticQuestionResponse res1 = room.getQuestions().get(0);
        assertThat(res1.getOrderNo()).isEqualTo(1);
        assertThat(res1.getImageUrl()).isEqualTo("https://cdn/p1.jpg");
        assertThat(res1.getAudioUrl()).isEqualTo("https://cdn/p1.mp3");
        assertThat(res1.getPassage()).isNull();

        DiagnosticQuestionResponse res2 = room.getQuestions().get(1);
        assertThat(res2.getOrderNo()).isEqualTo(2);
        assertThat(res2.getParentId()).isEqualTo(10L);
        assertThat(res2.getIsGroupQuestion()).isTrue();
        assertThat(res2.getAudioUrl()).isEqualTo("https://cdn/p3_shared.mp3");
        assertThat(res2.getInstruction()).isEqualTo("Questions 2 through 4 refer to the following conversation.");
        assertThat(res2.getPassage()).isNull(); // Listening không lộ passage

        DiagnosticQuestionResponse res3 = room.getQuestions().get(2);
        assertThat(res3.getOrderNo()).isEqualTo(3);
        assertThat(res3.getPartNo()).isEqualTo(7);
        assertThat(res3.getPassage()).isEqualTo("GLOBAL LOGISTICS NOTICE...");
    }
}
