package com.example.be.mapper.question;

import com.example.be.dto.response.question.QuestionExplanationResponse;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.mapper.question.ExamQuestionMapper.MappedQuestionContent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link ExamQuestionMapper}: kiểm tra trích xuất ngữ cảnh câu hỏi đơn/chùm,
 * bảo vệ chống lộ passage ở phần Listening, ánh xạ shuffle A-B-C-D (BR05) và giải thích kế thừa từ cha.
 */
class ExamQuestionMapperTest {

    private final ExamQuestionMapper mapper = new ExamQuestionMapper(new ObjectMapper());

    @Test
    void extractQuestionContent_remapsShuffledOptionsAndStripsPrefixCleanly() {
        Part part5 = new Part();
        part5.setId(5L);
        part5.setName("Part 5: Incomplete Sentences");
        part5.setSections(ESection.READING);

        Question q = new Question();
        q.setId(501L);
        q.setPart(part5);
        q.setSections(ESection.READING);
        q.setQuestionData("{\"text\":\"The team _____ the report yesterday.\",\"options\":[{\"key\":\"A\",\"text\":\"(A) finish\"},{\"key\":\"B\",\"text\":\"(B) finished\"},{\"key\":\"C\",\"text\":\"(C) finishing\"},{\"key\":\"D\",\"text\":\"(D) finishes\"}]}");
        q.setCorrectAnswer("{\"key\":\"B\"}");

        Map<String, String> optionMap = Map.of(
                "A", "B",
                "B", "A",
                "C", "D",
                "D", "C"
        );

        MappedQuestionContent content = mapper.extractQuestionContent(q, optionMap);

        assertThat(content.getPartNo()).isEqualTo(5);
        assertThat(content.getSection()).isEqualTo("READING");
        assertThat(content.getOptions()).extracting(opt -> opt.getKey() + ":" + opt.getText())
                .containsExactly("A:finished", "B:finish", "C:finishes", "D:finishing");

        assertThat(mapper.resolveOriginalKeyFromDisplay("A", optionMap)).isEqualTo("B");
        assertThat(mapper.resolveDisplayKeyFromOriginal("B", optionMap)).isEqualTo("A");
    }

    @Test
    void extractQuestionContent_mergesParentMediaAndExplanationFallbackWithoutLeakingListeningPassage() {
        Part part3 = new Part();
        part3.setId(3L);
        part3.setName("Part 3: Conversations");
        part3.setSections(ESection.LISTENING);

        Question parent = new Question();
        parent.setId(100L);
        parent.setPart(part3);
        parent.setSections(ESection.LISTENING);
        parent.setQuestionData("{\"instruction\":\"Listen to the conversation.\",\"passage\":\"Secret transcript\"}");
        parent.setExplanation("{\"transcript\":\"W: Hi\\nM: Hello\",\"translation\":\"Nữ: Chào\\nNam: Chào\"}");

        Media audio = Media.builder()
                .secureUrl("https://cdn/audio.mp3")
                .resourceType(EMediaResourceType.VIDEO)
                .format("mp3")
                .build();
        parent.setQuestionMedias(List.of(QuestionMedia.builder().question(parent).media(audio).orderNo(1).build()));

        Question child = new Question();
        child.setId(101L);
        child.setPart(part3);
        child.setSections(ESection.LISTENING);
        child.setParent(parent);
        child.setQuestionData("{\"text\":\"What are they talking about?\",\"options\":[{\"key\":\"A\",\"text\":\"Schedule\"}]}");
        child.setExplanation("{\"clue\":\"Nghe thấy Hi\",\"analysis\":\"Chọn A\"}");

        MappedQuestionContent content = mapper.extractQuestionContent(child, null);
        QuestionExplanationResponse explanation = mapper.resolveExplanation(child);

        assertThat(content.getIsGroupQuestion()).isTrue();
        assertThat(content.getAudioUrl()).isEqualTo("https://cdn/audio.mp3");
        assertThat(content.getInstruction()).isEqualTo("Listen to the conversation.");
        assertThat(content.getPassage()).isNull();

        assertThat(explanation).isNotNull();
        assertThat(explanation.getTranscript()).isEqualTo("W: Hi\nM: Hello");
        assertThat(explanation.getTranslation()).isEqualTo("Nữ: Chào\nNam: Chào");
        assertThat(explanation.getClue()).isEqualTo("Nghe thấy Hi");
    }
}
