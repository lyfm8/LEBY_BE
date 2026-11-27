package com.example.be.mapper.diagnostic;

import com.example.be.dto.response.diagnostic.DiagnosticResultDetailResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.ability.UserAbility;
import com.example.be.entity.content.LearningPath;
import com.example.be.entity.content.LearningPathItem;
import com.example.be.entity.content.Part;
import com.example.be.entity.diagnostic.DiagnosticAnswer;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.user.TargetProfile;
import com.example.be.enums.ability.EAbilityStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link DiagnosticResultMapper} phục vụ báo cáo UC07 và quy đổi điểm TOEIC 990.
 */
class DiagnosticResultMapperTest {

    private final DiagnosticResultMapper mapper = new DiagnosticResultMapper();

    @Test
    void calculateScaledToeicScore_roundsToMultipleOf5Within0To990() {
        assertThat(mapper.calculateScaledToeicScore(0, 50, 990)).isEqualTo(0);
        assertThat(mapper.calculateScaledToeicScore(50, 50, 990)).isEqualTo(990);
        assertThat(mapper.calculateScaledToeicScore(30, 50, 990)).isEqualTo(595); // 594 -> 595
        assertThat(mapper.calculateScaledToeicScore(13, 17, 990)).isEqualTo(755); // 757.05 -> 755
        assertThat(mapper.calculateScaledToeicScore(17, 17, 990) % 5).isZero();
    }

    @Test
    void toDetailResponse_buildsPartBreakdownRadarAndWeakAbilities() {
        Part part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1: Photographs");

        Part part5 = new Part();
        part5.setId(5L);
        part5.setName("Part 5: Incomplete Sentences");

        Ability ab1 = new Ability();
        ab1.setId(10L);
        ab1.setName("Nghe tranh có người");

        Ability ab5 = new Ability();
        ab5.setId(50L);
        ab5.setName("Ngữ pháp từ loại");

        Question q1 = createQuestion(1L, part1, ab1);
        Question q2 = createQuestion(2L, part5, ab5);

        DiagnosticAnswer a1 = new DiagnosticAnswer();
        a1.setQuestion(q1);
        a1.setIsCorrect(true);

        DiagnosticAnswer a2 = new DiagnosticAnswer();
        a2.setQuestion(q2);
        a2.setIsCorrect(false);

        PartDiagnosticResult pr1 = new PartDiagnosticResult();
        pr1.setPart(part1);
        pr1.setScore(100.0f);
        pr1.setDirective(EPartDirective.PASS);

        PartDiagnosticResult pr5 = new PartDiagnosticResult();
        pr5.setPart(part5);
        pr5.setScore(0.0f);
        pr5.setDirective(EPartDirective.FULL_PART);

        UserAbility ua5 = new UserAbility();
        ua5.setAbility(ab5);
        ua5.setPart(part5);
        ua5.setStatus(EAbilityStatus.WEAK);

        TargetProfile target = new TargetProfile();
        target.setId(3L);
        target.setTargetTotalScore(650);

        LearningPath lp = new LearningPath();
        lp.setId(88L);
        lp.setItems(List.of(new LearningPathItem(), new LearningPathItem()));

        DiagnosticTest test = new DiagnosticTest();
        test.setId(1L);
        test.setTitle("Bài thi chẩn đoán đầu vào");

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(500L);
        attempt.setDiagnosticTest(test);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setDurationSeconds(1200);

        DiagnosticResultDetailResponse detail = mapper.toDetailResponse(
                attempt,
                List.of(pr5, pr1),
                List.of(a1, a2),
                List.of(q1, q2),
                List.of(ua5),
                List.of(),
                target,
                lp
        );

        assertThat(detail.getAttemptId()).isEqualTo(500L);
        assertThat(detail.getLearningPathId()).isEqualTo(88L);
        assertThat(detail.getTotalQuestions()).isEqualTo(2);
        assertThat(detail.getCorrectAnswers()).isEqualTo(1);
        assertThat(detail.getPredictedScore()).isEqualTo(495);
        assertThat(detail.getTargetScore()).isEqualTo(650);

        assertThat(detail.getSectionScores().getListeningPercent()).isEqualTo(100);
        assertThat(detail.getSectionScores().getReadingPercent()).isEqualTo(0);

        assertThat(detail.getPartResults()).hasSize(2);
        assertThat(detail.getPartResults().get(0).getPartNo()).isEqualTo(1);
        assertThat(detail.getPartResults().get(0).getDirectiveLabel()).isEqualTo("Đạt chuẩn");
        assertThat(detail.getPartResults().get(0).getAbilities()).hasSize(1);
        assertThat(detail.getPartResults().get(1).getPartNo()).isEqualTo(5);
        assertThat(detail.getPartResults().get(1).getDirectiveLabel()).isEqualTo("Mất gốc");

        assertThat(detail.getAbilitiesRadar()).hasSize(2);
        assertThat(detail.getPassedParts()).containsExactly("Part 1: Photographs");
        assertThat(detail.getWeakAbilities()).hasSize(1);
        assertThat(detail.getWeakAbilities().get(0).getAbilityName()).isEqualTo("Ngữ pháp từ loại");
        assertThat(detail.getAiRoadmapSummary().getFocusParts()).containsExactly(5);

        DiagnosticResultDetailResponse withoutRoadmap = mapper.toDetailResponse(
                attempt,
                List.of(pr5, pr1),
                List.of(a1, a2),
                List.of(q1, q2),
                List.of(ua5),
                List.of(),
                target,
                null
        );
        assertThat(withoutRoadmap.getLearningPathId()).isNull();
        assertThat(withoutRoadmap.getAiRoadmapSummary().getRadarInsight()).doesNotContain("0 mô-đun trọng tâm");
    }

    @Test
    void toDetailResponse_whenListeningAndReadingAreEqualZero_buildsNaturalStarterInsight() {
        Part part1 = new Part();
        part1.setId(1L);
        part1.setName("Part 1: Photographs");

        Part part5 = new Part();
        part5.setId(5L);
        part5.setName("Part 5: Incomplete Sentences");

        Ability ab1 = new Ability();
        ab1.setId(10L);
        ab1.setName("Nghe tranh");

        Ability ab5 = new Ability();
        ab5.setId(50L);
        ab5.setName("Ngữ pháp");

        Question q1 = createQuestion(1L, part1, ab1);
        Question q2 = createQuestion(2L, part5, ab5);

        DiagnosticAnswer a1 = new DiagnosticAnswer();
        a1.setQuestion(q1);
        a1.setIsCorrect(false);

        DiagnosticAnswer a2 = new DiagnosticAnswer();
        a2.setQuestion(q2);
        a2.setIsCorrect(false);

        PartDiagnosticResult pr1 = new PartDiagnosticResult();
        pr1.setPart(part1);
        pr1.setScore(0.0f);
        pr1.setDirective(EPartDirective.FULL_PART);

        PartDiagnosticResult pr5 = new PartDiagnosticResult();
        pr5.setPart(part5);
        pr5.setScore(0.0f);
        pr5.setDirective(EPartDirective.FULL_PART);

        TargetProfile target = new TargetProfile();
        target.setId(3L);
        target.setTargetTotalScore(650);

        DiagnosticTest test = new DiagnosticTest();
        test.setId(1L);
        test.setTitle("Bài thi chẩn đoán đầu vào");

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(501L);
        attempt.setDiagnosticTest(test);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setDurationSeconds(600);

        DiagnosticResultDetailResponse detail = mapper.toDetailResponse(
                attempt,
                List.of(pr1, pr5),
                List.of(a1, a2),
                List.of(q1, q2),
                List.of(),
                List.of(),
                target,
                null
        );

        String insight = detail.getAiRoadmapSummary().getRadarInsight();
        assertThat(insight).doesNotContain("nhỉnh hơn");
        assertThat(insight).contains("mức khởi đầu (0%)");
    }

    private Question createQuestion(Long id, Part part, Ability ability) {
        Question q = new Question();
        q.setId(id);
        q.setPart(part);
        QuestionAbility qa = new QuestionAbility();
        qa.setAbility(ability);
        q.setQuestionAbilities(List.of(qa));
        return q;
    }
}
