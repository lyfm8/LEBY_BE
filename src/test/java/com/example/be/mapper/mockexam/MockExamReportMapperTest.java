package com.example.be.mapper.mockexam;

import com.example.be.dto.response.mockexam.MockExamReportResponse;
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
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.mapper.question.ExamQuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link MockExamReportMapper}:
 * - Quy đổi điểm chuẩn ETS 5 - 495 từ bảng {@link ToeicScoreConversion}.
 * - Đề PRACTICE không kết xuất Radar Trước vs Sau hay khuyến nghị đầu ra.
 * - Đề OUTPUT kết xuất đầy đủ Radar Trước vs Sau (BR15), Ability breakdown và khuyến nghị UC18.
 */
class MockExamReportMapperTest {

    private MockExamReportMapper mapper;
    private Map<Integer, ToeicScoreConversion> conversionMap;
    private TargetProfile target750;
    private List<Part> parts;

    @BeforeEach
    void setUp() {
        mapper = new MockExamReportMapper(new ExamQuestionMapper(new ObjectMapper()));

        conversionMap = new HashMap<>();
        ToeicScoreConversion conv0 = new ToeicScoreConversion();
        conv0.setCorrectCount(0);
        conv0.setListeningScore(5);
        conv0.setReadingScore(5);
        conversionMap.put(0, conv0);

        ToeicScoreConversion conv50 = new ToeicScoreConversion();
        conv50.setCorrectCount(50);
        conv50.setListeningScore(250);
        conv50.setReadingScore(220);
        conversionMap.put(50, conv50);

        ToeicScoreConversion conv100 = new ToeicScoreConversion();
        conv100.setCorrectCount(100);
        conv100.setListeningScore(495);
        conv100.setReadingScore(495);
        conversionMap.put(100, conv100);

        target750 = new TargetProfile();
        target750.setId(4L);
        target750.setTargetTotalScore(750);

        parts = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            Part p = new Part();
            p.setId((long) i);
            p.setName("Part " + i);
            parts.add(p);
        }
    }

    @Test
    void toReportResponse_practiceExam_omitsBeforeAfterRadarAndRecommendations() {
        DiagnosticTest practiceTest = new DiagnosticTest();
        practiceTest.setId(10L);
        practiceTest.setTitle("ETS Practice Mock Test");
        practiceTest.setTestType(ETestType.FULL_MOCK_TEST);
        practiceTest.setExamTag(EExamTag.PRACTICE);

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(50L);
        attempt.setDiagnosticTest(practiceTest);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setDurationSeconds(5400);

        List<Question> questions = new ArrayList<>();
        List<DiagnosticAnswer> answers = new ArrayList<>();
        // 1 câu Listening (Part 1) đúng, 1 câu Reading (Part 5) sai -> scale 100% Listening (495) + 0% Reading (5) = 500
        Question q1 = createQuestion(1L, parts.get(0), null);
        Question q2 = createQuestion(2L, parts.get(4), null);
        questions.add(q1);
        questions.add(q2);
        answers.add(createAnswer(q1, true));
        answers.add(createAnswer(q2, false));

        MockExamReportResponse report = mapper.toReportResponse(
                attempt,
                Collections.emptyList(),
                answers,
                questions,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                conversionMap,
                target750
        );

        assertThat(report.getExamTag()).isEqualTo(EExamTag.PRACTICE);
        assertThat(report.getListeningScore()).isEqualTo(495);
        assertThat(report.getReadingScore()).isEqualTo(5);
        assertThat(report.getTotalToeicScore()).isEqualTo(500);
        assertThat(report.getCanViewReview()).isTrue();
        assertThat(report.getBeforeRadar()).isNull();
        assertThat(report.getAfterRadar()).isNull();
        assertThat(report.getRemainingWeakAbilities()).isEmpty();
        assertThat(report.getRecommendations()).isEmpty();
        assertThat(report.getSummaryHeadline()).isNull();
        assertThat(report.getPartResults()).hasSize(7);
        assertThat(report.getPartResults().get(0).getBeforeScore()).isNull();
    }

    @Test
    void toReportResponse_outputExam_populatesBeforeAfterRadarComparisonAndRecommendations() {
        DiagnosticTest outputTest = new DiagnosticTest();
        outputTest.setId(2L);
        outputTest.setTitle("Đề thi thử đầu ra ETS 2026");
        outputTest.setTestType(ETestType.FULL_MOCK_TEST);
        outputTest.setExamTag(EExamTag.OUTPUT);

        DiagnosticAttempt attempt = new DiagnosticAttempt();
        attempt.setId(99L);
        attempt.setDiagnosticTest(outputTest);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setDurationSeconds(7000);

        Ability abListen = new Ability();
        abListen.setId(1L);
        abListen.setName("Nghe tranh");

        Ability abGrammar = new Ability();
        abGrammar.setId(5L);
        abGrammar.setName("Từ loại và Ngữ pháp");

        AbilityEvaluationRule ruleGrammar = new AbilityEvaluationRule();
        ruleGrammar.setAbility(abGrammar);
        ruleGrammar.setDevelopingThreshold(50.0f);
        ruleGrammar.setStableThreshold(75.0f);

        List<Question> questions = new ArrayList<>();
        List<DiagnosticAnswer> answers = new ArrayList<>();
        // Part 1: 1/1 đúng (100%); Part 5: 0/1 đúng (0% -> WEAK)
        Question q1 = createQuestion(1L, parts.get(0), abListen);
        Question q5 = createQuestion(2L, parts.get(4), abGrammar);
        questions.add(q1);
        questions.add(q5);
        answers.add(createAnswer(q1, true));
        answers.add(createAnswer(q5, false));

        List<PartDiagnosticResult> beforeResults = new ArrayList<>();
        for (Part part : parts) {
            PartDiagnosticResult pr = new PartDiagnosticResult();
            pr.setPart(part);
            pr.setScore(part.getId() == 1L ? 40.0f : 30.0f);
            pr.setDirective(EPartDirective.ABILITY_DIAGNOSTIC);
            beforeResults.add(pr);
        }

        Map<Long, TargetPartThreshold> thresholds = new HashMap<>();
        TargetPartThreshold p1Thresh = new TargetPartThreshold();
        p1Thresh.setPart(parts.get(0));
        p1Thresh.setPassScore(80.0f);
        thresholds.put(1L, p1Thresh);

        MockExamReportResponse report = mapper.toReportResponse(
                attempt,
                Collections.emptyList(),
                answers,
                questions,
                beforeResults,
                List.of(ruleGrammar),
                thresholds,
                conversionMap,
                target750
        );

        assertThat(report.getExamTag()).isEqualTo(EExamTag.OUTPUT);
        assertThat(report.getTotalToeicScore()).isEqualTo(500);
        assertThat(report.getTargetScore()).isEqualTo(750);
        assertThat(report.getIsAimAchieved()).isFalse();
        assertThat(report.getCanViewReview()).isFalse();
        assertThat(report.getScoreDifference()).isEqualTo(-250);
        assertThat(report.getBeforeRadar()).hasSize(7);
        assertThat(report.getAfterRadar()).hasSize(7);
        assertThat(report.getBeforeRadar().get(0).getScore()).isEqualTo(40);
        assertThat(report.getAfterRadar().get(0).getScore()).isEqualTo(100);
        assertThat(report.getPartResults().get(0).getScoreDelta()).isEqualTo(60);
        assertThat(report.getRemainingWeakAbilities()).hasSize(1);
        assertThat(report.getRemainingWeakAbilities().get(0).getAbilityName()).isEqualTo("Từ loại và Ngữ pháp");
        assertThat(report.getRecommendations()).isNotEmpty();
        assertThat(report.getSummaryHeadline()).contains("750");
    }

    private Question createQuestion(Long id, Part part, Ability ability) {
        Question q = new Question();
        q.setId(id);
        q.setPart(part);
        if (ability != null) {
            QuestionAbility qa = new QuestionAbility();
            qa.setAbility(ability);
            q.setQuestionAbilities(List.of(qa));
        }
        return q;
    }

    private DiagnosticAnswer createAnswer(Question question, boolean isCorrect) {
        DiagnosticAnswer ans = new DiagnosticAnswer();
        ans.setQuestion(question);
        ans.setIsCorrect(isCorrect);
        return ans;
    }
}
