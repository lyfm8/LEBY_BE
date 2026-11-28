package com.example.be.service.content.implement;

import com.example.be.dto.request.content.SubmitMistakeRetryRequest;
import com.example.be.dto.request.content.SubmitMistakeRetryRequest.MistakeRetryAnswerItem;
import com.example.be.dto.response.content.MistakeNotebookResponse;
import com.example.be.dto.response.content.MistakeNotebookResponse.AbilityCountItem;
import com.example.be.dto.response.content.MistakeNotebookResponse.FilterOptions;
import com.example.be.dto.response.content.MistakeNotebookResponse.PartCountItem;
import com.example.be.dto.response.content.MistakeNotebookResponse.SourceCountItem;
import com.example.be.dto.response.content.MistakeQuestionItemResponse;
import com.example.be.dto.response.content.MistakeQuestionItemResponse.AbilityTag;
import com.example.be.dto.response.content.MistakeRetrySetResponse;
import com.example.be.dto.response.content.MistakeRetrySetResponse.RetryQuestionItem;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse;
import com.example.be.dto.response.content.MistakeRetrySubmitResponse.QuestionRetryResult;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.ToeicScoreConversion;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.EMistakeSourceType;
import com.example.be.enums.diagnostic.EAttemptStatus;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.UserMistakeNotebookMapper;
import com.example.be.mapper.mockexam.MockExamReportMapper;
import com.example.be.repository.content.UserMistakeQueryRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.setting.ToeicScoreConversionRepository;
import com.example.be.service.content.service.UserMistakeNotebookService;
import com.example.be.service.exam.util.QuestionBatchLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Sổ tay câu hỏi làm sai và phiên làm lại trực tiếp (UC15).
 * Gom lỗi sai từ lịch sử bài thi chẩn đoán, bài thi vượt ải mô-đun và bài thi thử TOEIC
 * theo cơ chế chỉ đọc (0 bảng DB thừa, 0 N+1 Query nhờ tái sử dụng {@link QuestionBatchLoader}),
 * đồng thời áp dụng chặt quy tắc khóa đáp án với bài đầu ra OUTPUT chưa đạt AIM và bài chẩn đoán chưa mở review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMistakeNotebookServiceImpl implements UserMistakeNotebookService {

    private static final List<EAttemptStatus> COMPLETED_ATTEMPT_STATUSES = List.of(
            EAttemptStatus.SUBMITTED,
            EAttemptStatus.TIMED_OUT
    );
    private static final int LISTENING_PART_MAX = 4;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_RETRY_LIMIT = 20;
    private static final int MAX_RETRY_LIMIT = 50;
    private static final int FULL_PERCENT = 100;

    private final UserMistakeQueryRepository userMistakeQueryRepository;
    private final QuestionAbilityRepository questionAbilityRepository;
    private final ToeicScoreConversionRepository toeicScoreConversionRepository;
    private final QuestionBatchLoader questionBatchLoader;
    private final MockExamReportMapper mockExamReportMapper;
    private final UserMistakeNotebookMapper userMistakeNotebookMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MistakeNotebookResponse getMistakeNotebook(
            Long userId,
            Long partId,
            Long abilityId,
            EMistakeSourceType sourceType,
            String keyword,
            int page,
            int size
    ) {
        // Kẹp biên phân trang an toàn phòng trường hợp caller truyền giá trị ngoài ngưỡng
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size > 0 ? size : DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);

        // Bước 1: Gom và khử trùng lặp toàn bộ câu làm sai từ Bài thi vượt ải & Bài thi thử TOEIC
        List<AggregatedMistake> allMistakes = loadAndAggregateUserMistakes(userId);
        if (allMistakes.isEmpty()) {
            // Trả sớm khi sổ tay rỗng (UC15 Luồng 5b), không bắn thêm truy vấn thừa
            return buildEmptyNotebookResponse(safePage, safeSize);
        }

        // Bước 2: Nạp metadata nhẹ (Part, tên câu hỏi, Ability) để dựng bộ lọc và đếm số lượng chính xác
        List<Long> allQuestionIds = allMistakes.stream().map(AggregatedMistake::questionId).toList();
        Map<Long, QuestionMeta> metaByQuestionId = loadQuestionMetaMap(allQuestionIds);
        Map<Long, List<AbilityTag>> abilitiesByQuestionId = loadAbilityTagsMap(allQuestionIds);
        FilterOptions filterOptions = buildFilterOptions(allMistakes, metaByQuestionId, abilitiesByQuestionId);

        // Bước 3: Lọc danh sách câu sai khớp tiêu chí Part / Ability / Nguồn / Từ khóa
        List<AggregatedMistake> filtered = filterMistakes(
                allMistakes,
                metaByQuestionId,
                abilitiesByQuestionId,
                partId,
                abilityId,
                sourceType,
                keyword
        );

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = safePage * safeSize;

        // Nếu chỉ số trang vượt quá số phần tử sau lọc thì trả trang rỗng kèm bộ lọc đầy đủ
        if (fromIndex >= totalElements) {
            return MistakeNotebookResponse.builder()
                    .totalWrongCount(allMistakes.size())
                    .filterOptions(filterOptions)
                    .items(Collections.emptyList())
                    .page(safePage)
                    .size(safeSize)
                    .totalElements((long) totalElements)
                    .totalPages(totalPages)
                    .hasNext(false)
                    .hasPrevious(safePage > 0 && totalElements > 0)
                    .build();
        }

        // Bước 4: Cắt lát trang hiện tại và chỉ nạp chi tiết Question/Parent/Media cho đúng trang này (2 query batch)
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        List<AggregatedMistake> pageSlice = filtered.subList(fromIndex, toIndex);
        List<Long> pageQuestionIds = pageSlice.stream().map(AggregatedMistake::questionId).toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(pageQuestionIds);

        // Bước 5: Ánh xạ từng câu sang DTO đầy đủ kèm đáp án đã chọn sai, đáp án đúng và lời giải BR14
        List<MistakeQuestionItemResponse> items = new ArrayList<>(pageSlice.size());
        for (AggregatedMistake mistake : pageSlice) {
            Question question = detailedQuestionMap.get(mistake.questionId());
            if (question == null) {
                continue;
            }
            items.add(userMistakeNotebookMapper.toMistakeItemResponse(
                    question,
                    abilitiesByQuestionId.getOrDefault(mistake.questionId(), Collections.emptyList()),
                    mistake.latestSourceType(),
                    mistake.latestSourceLabel(),
                    mistake.wrongCount(),
                    mistake.lastWrongAt(),
                    mistake.lastUserAnswer()
            ));
        }

        return MistakeNotebookResponse.builder()
                .totalWrongCount(allMistakes.size())
                .filterOptions(filterOptions)
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalElements((long) totalElements)
                .totalPages(totalPages)
                .hasNext(safePage + 1 < totalPages)
                .hasPrevious(safePage > 0)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MistakeRetrySetResponse getRetryQuestionSet(
            Long userId,
            Long partId,
            Long abilityId,
            EMistakeSourceType sourceType,
            String keyword,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(1, limit > 0 ? limit : DEFAULT_RETRY_LIMIT), MAX_RETRY_LIMIT);

        // Bước 1: Gom toàn bộ câu sai của học viên và lọc theo đúng bộ lọc đang chọn trên Sổ tay
        List<AggregatedMistake> allMistakes = loadAndAggregateUserMistakes(userId);
        if (allMistakes.isEmpty()) {
            return MistakeRetrySetResponse.builder()
                    .totalAvailable(0)
                    .sessionSize(0)
                    .questions(Collections.emptyList())
                    .build();
        }

        List<Long> allQuestionIds = allMistakes.stream().map(AggregatedMistake::questionId).toList();
        Map<Long, QuestionMeta> metaByQuestionId = loadQuestionMetaMap(allQuestionIds);
        Map<Long, List<AbilityTag>> abilitiesByQuestionId = loadAbilityTagsMap(allQuestionIds);
        List<AggregatedMistake> filtered = filterMistakes(
                allMistakes,
                metaByQuestionId,
                abilitiesByQuestionId,
                partId,
                abilityId,
                sourceType,
                keyword
        );

        if (filtered.isEmpty()) {
            return MistakeRetrySetResponse.builder()
                    .totalAvailable(0)
                    .sessionSize(0)
                    .questions(Collections.emptyList())
                    .build();
        }

        // Bước 2: Lấy tối đa safeLimit câu để nạp batch nội dung đề bài (tuyệt đối ẩn đáp án và lời giải)
        List<AggregatedMistake> selected = filtered.subList(0, Math.min(safeLimit, filtered.size()));
        List<Long> selectedIds = selected.stream().map(AggregatedMistake::questionId).toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(selectedIds);

        List<RetryQuestionItem> retryQuestions = new ArrayList<>(selected.size());
        int orderNo = 1;
        for (AggregatedMistake mistake : selected) {
            Question question = detailedQuestionMap.get(mistake.questionId());
            if (question == null) {
                continue;
            }
            retryQuestions.add(userMistakeNotebookMapper.toRetryQuestionItem(
                    orderNo++,
                    question,
                    abilitiesByQuestionId.getOrDefault(mistake.questionId(), Collections.emptyList()),
                    mistake.latestSourceType(),
                    mistake.latestSourceLabel(),
                    mistake.wrongCount()
            ));
        }

        return MistakeRetrySetResponse.builder()
                .totalAvailable(filtered.size())
                .sessionSize(retryQuestions.size())
                .questions(retryQuestions)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MistakeRetrySubmitResponse submitRetryAnswers(Long userId, SubmitMistakeRetryRequest request) {
        // Bước 1: Khử trùng lặp questionId trong payload nộp lên (giữ phương án chọn sau cùng nếu bị gửi trùng)
        Map<Long, String> submittedByQuestionId = new LinkedHashMap<>();
        for (MistakeRetryAnswerItem item : request.getAnswers()) {
            submittedByQuestionId.put(item.getQuestionId(), item.getSelectedAnswer());
        }

        // Bước 2: SECURITY (Chống IDOR / Answer-harvesting) — xác minh mọi câu nộp đều thuộc sổ tay câu sai của user
        Set<Long> ownedWrongQuestionIds = loadOwnedWrongQuestionIds(userId);
        for (Long submittedId : submittedByQuestionId.keySet()) {
            if (!ownedWrongQuestionIds.contains(submittedId)) {
                log.warn("[UC15] User id={} cố gửi questionId={} không thuộc sổ tay câu sai", userId, submittedId);
                throw new BaseException(
                        ErrorCode.FORBIDDEN,
                        "Bạn chỉ có thể chấm điểm các câu hỏi nằm trong sổ tay câu sai của mình"
                );
            }
        }

        // Bước 3: Nạp hàng loạt Question kèm Parent (để hợp nhất lời giải BR14 từ câu cha) trong 2 query
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(
                submittedByQuestionId.keySet()
        );

        // Bước 4: Chấm từng câu phía server và tổng hợp số câu đã trả lời, số câu đúng, tỷ lệ chính xác
        List<QuestionRetryResult> results = new ArrayList<>(submittedByQuestionId.size());
        int answeredCount = 0;
        int correctCount = 0;

        for (Map.Entry<Long, String> entry : submittedByQuestionId.entrySet()) {
            Question question = detailedQuestionMap.get(entry.getKey());
            if (question == null) {
                continue;
            }
            QuestionRetryResult result = userMistakeNotebookMapper.toQuestionRetryResult(question, entry.getValue());
            if (result.getSelectedAnswer() != null) {
                answeredCount++;
            }
            if (Boolean.TRUE.equals(result.getIsCorrect())) {
                correctCount++;
            }
            results.add(result);
        }

        int totalQuestions = results.size();
        int accuracyPercent = totalQuestions > 0
                ? (int) Math.round((correctCount * (double) FULL_PERCENT) / totalQuestions)
                : 0;

        log.info("[UC15] User id={} nộp bài làm lại sổ tay câu sai: {}/{} câu đúng ({}%)",
                userId, correctCount, totalQuestions, accuracyPercent);

        return MistakeRetrySubmitResponse.builder()
                .totalQuestions(totalQuestions)
                .answeredCount(answeredCount)
                .correctCount(correctCount)
                .accuracyPercent(accuracyPercent)
                .results(results)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS — GOM DỮ LIỆU, LỌC & THỐNG KÊ BỘ ĐẾM
    // =========================================================================

    /**
     * Gom và khử trùng lặp toàn bộ câu hỏi sai của học viên từ đúng 2 nguồn học tập & khảo thí:
     * 1) Bài thi thử TOEIC (FULL_MOCK_TEST) — đề PRACTICE luôn mở; đề đầu ra OUTPUT chỉ mở khi đạt mục tiêu AIM.
     * 2) Bài thi vượt ải mô-đun (MODULE_TEST).
     * Tuyệt đối không đưa câu hỏi từ bài thi chẩn đoán đầu vào (DIAGNOSTIC) vào Sổ tay câu sai.
     */
    private List<AggregatedMistake> loadAndAggregateUserMistakes(Long userId) {
        Map<Long, MutableMistakeAccumulator> accByQuestionId = new LinkedHashMap<>();

        // Nguồn 1: Lỗi sai từ bài thi thử TOEIC 200 câu đã nộp (chỉ truy vấn FULL_MOCK_TEST)
        List<Object[]> mockExamRows = userMistakeQueryRepository.findWrongMockExamAnswersByUserId(
                userId,
                ETestType.FULL_MOCK_TEST,
                COMPLETED_ATTEMPT_STATUSES
        );

        // Gom các lượt thi đầu ra (OUTPUT) để kiểm tra điều kiện mở khóa theo điểm AIM trong đúng 1 câu SQL
        Map<Long, Integer> outputAttemptTargetScores = new HashMap<>();
        if (mockExamRows != null) {
            for (Object[] row : mockExamRows) {
                if (row == null || row.length < 7) {
                    continue;
                }
                Long attemptId = toLong(row[3]);
                EExamTag examTag = row[5] instanceof EExamTag tag ? tag : null;
                if (examTag == EExamTag.OUTPUT && attemptId != null) {
                    Integer targetScore = row[6] instanceof Number num ? num.intValue() : null;
                    outputAttemptTargetScores.putIfAbsent(attemptId, targetScore);
                }
            }
        }
        Set<Long> unlockedOutputAttemptIds = resolveUnlockedOutputAttemptIds(outputAttemptTargetScores);

        if (mockExamRows != null) {
            for (Object[] row : mockExamRows) {
                if (row == null || row.length < 5) {
                    continue;
                }
                Long questionId = toLong(row[0]);
                if (questionId == null) {
                    continue;
                }

                Long attemptId = toLong(row[3]);
                EExamTag examTag = row.length > 5 && row[5] instanceof EExamTag tag ? tag : null;

                // Chốt chặn bảo mật: bỏ qua câu sai của bài thi đầu ra (OUTPUT) chưa đạt mục tiêu AIM
                if (examTag == EExamTag.OUTPUT && (attemptId == null || !unlockedOutputAttemptIds.contains(attemptId))) {
                    continue;
                }

                String userAnswer = userMistakeNotebookMapper.extractAnswerKey(toStringOrNull(row[1]));
                String sourceLabel = toStringOrNull(row[2]);
                LocalDateTime answeredAt = toLocalDateTime(row[4]);

                mergeMistakeOccurrence(
                        accByQuestionId,
                        questionId,
                        EMistakeSourceType.FULL_MOCK_TEST,
                        sourceLabel,
                        userAnswer,
                        answeredAt
                );
            }
        }

        // Nguồn 2: Lỗi sai từ bài thi vượt ải mô-đun đã nộp (lấy trực tiếp a.question.id theo schema V6)
        List<Object[]> moduleTestRows = userMistakeQueryRepository.findWrongModuleTestAnswersByUserId(
                userId,
                COMPLETED_ATTEMPT_STATUSES
        );
        if (moduleTestRows != null) {
            for (Object[] row : moduleTestRows) {
                Long questionId = toLong(row[0]);
                if (questionId == null) {
                    continue;
                }
                String userAnswer = userMistakeNotebookMapper.extractAnswerKey(toStringOrNull(row[1]));
                String moduleTitle = toStringOrNull(row[2]);
                String sourceLabel = moduleTitle != null && !moduleTitle.isBlank()
                        ? "Thi vượt ải: " + moduleTitle.trim()
                        : null;
                LocalDateTime answeredAt = toLocalDateTime(row[4]);

                mergeMistakeOccurrence(
                        accByQuestionId,
                        questionId,
                        EMistakeSourceType.MODULE_TEST,
                        sourceLabel,
                        userAnswer,
                        answeredAt
                );
            }
        }

        // Sắp xếp ổn định: câu vừa làm sai gần nhất lên đầu, cùng thời điểm thì ưu tiên câu sai nhiều lần hơn
        return accByQuestionId.values().stream()
                .map(MutableMistakeAccumulator::toImmutable)
                .sorted(Comparator
                        .comparing(AggregatedMistake::lastWrongAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AggregatedMistake::wrongCount, Comparator.reverseOrder())
                        .thenComparing(AggregatedMistake::questionId))
                .toList();
    }

    /**
     * Xác định tập hợp các lượt thi đầu ra (OUTPUT) đã đạt mục tiêu AIM (mở khóa xem đáp án)
     * bằng đúng 1 câu truy vấn tổng hợp theo Part + bảng quy đổi ETS 990 (0 N+1).
     */
    private Set<Long> resolveUnlockedOutputAttemptIds(Map<Long, Integer> outputAttemptTargetScores) {
        if (outputAttemptTargetScores.isEmpty()) {
            return Collections.emptySet();
        }

        List<Object[]> statRows = userMistakeQueryRepository.countOutputAttemptPartStatsByAttemptIds(
                outputAttemptTargetScores.keySet()
        );
        Map<Long, OutputAttemptSectionCounter> counterByAttemptId = new HashMap<>();
        if (statRows != null) {
            for (Object[] row : statRows) {
                if (row == null || row.length < 6) {
                    continue;
                }
                Long attemptId = toLong(row[0]);
                Long partId = toLong(row[1]);
                Object sectionObj = row[2];
                String partName = toStringOrNull(row[3]);
                int total = row[4] instanceof Number num ? num.intValue() : 0;
                int correct = row[5] instanceof Number num ? num.intValue() : 0;
                if (attemptId == null || total <= 0) {
                    continue;
                }
                OutputAttemptSectionCounter counter = counterByAttemptId.computeIfAbsent(
                        attemptId,
                        ignored -> new OutputAttemptSectionCounter()
                );
                if (isListeningSection(sectionObj, partName, partId)) {
                    counter.listeningTotal += total;
                    counter.listeningCorrect += correct;
                } else {
                    counter.readingTotal += total;
                    counter.readingCorrect += correct;
                }
            }
        }

        Map<Integer, ToeicScoreConversion> conversionMap = loadToeicScoreConversionMap();
        Set<Long> unlockedIds = new HashSet<>();

        for (Map.Entry<Long, Integer> entry : outputAttemptTargetScores.entrySet()) {
            Long attemptId = entry.getKey();
            Integer targetScore = entry.getValue();
            OutputAttemptSectionCounter counter = counterByAttemptId.get(attemptId);
            if (counter == null) {
                continue;
            }
            int listeningScore = mockExamReportMapper.resolveSectionToeicScore(
                    counter.listeningCorrect,
                    counter.listeningTotal,
                    true,
                    conversionMap
            );
            int readingScore = mockExamReportMapper.resolveSectionToeicScore(
                    counter.readingCorrect,
                    counter.readingTotal,
                    false,
                    conversionMap
            );
            int totalToeicScore = listeningScore + readingScore;
            if (MockExamReportMapper.isReviewUnlocked(EExamTag.OUTPUT, totalToeicScore, targetScore)) {
                unlockedIds.add(attemptId);
            }
        }
        return unlockedIds;
    }

    /**
     * Phân loại kỹ năng Nghe (LISTENING) hay Đọc (READING) ưu tiên theo cột {@code parts.sections},
     * dự phòng phân tích tên Part nếu dữ liệu section bị thiếu.
     */
    private boolean isListeningSection(Object sectionObj, String partName, Long partId) {
        if (sectionObj instanceof ESection section) {
            return section == ESection.LISTENING;
        }
        if (sectionObj != null) {
            return "LISTENING".equalsIgnoreCase(sectionObj.toString());
        }
        if (partName != null && !partName.isBlank()) {
            return partName.contains("1") || partName.contains("2")
                    || partName.contains("3") || partName.contains("4");
        }
        return partId != null && partId <= LISTENING_PART_MAX;
    }

    private Map<Integer, ToeicScoreConversion> loadToeicScoreConversionMap() {
        List<ToeicScoreConversion> rows = toeicScoreConversionRepository.findAll();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream()
                .filter(row -> row != null && row.getCorrectCount() != null)
                .collect(Collectors.toMap(
                        ToeicScoreConversion::getCorrectCount,
                        row -> row,
                        (first, second) -> first
                ));
    }

    /**
     * Lấy tập ID các câu hỏi sai thuộc quyền sở hữu của user để kiểm tra nhanh chống IDOR khi chấm làm lại.
     */
    private Set<Long> loadOwnedWrongQuestionIds(Long userId) {
        return loadAndAggregateUserMistakes(userId).stream()
                .map(AggregatedMistake::questionId)
                .collect(Collectors.toSet());
    }

    /**
     * Gộp 1 lần làm sai vào bộ tích lũy theo {@code questionId}: cộng dồn {@code wrongCount},
     * lưu đủ các nguồn từng sai và giữ nguyên thông tin của lần sai có mốc thời gian mới nhất.
     */
    private void mergeMistakeOccurrence(
            Map<Long, MutableMistakeAccumulator> accMap,
            Long questionId,
            EMistakeSourceType sourceType,
            String sourceLabel,
            String userAnswer,
            LocalDateTime answeredAt
    ) {
        MutableMistakeAccumulator acc = accMap.get(questionId);
        if (acc == null) {
            acc = new MutableMistakeAccumulator(questionId, sourceType, sourceLabel, userAnswer, answeredAt);
            accMap.put(questionId, acc);
            return;
        }
        acc.recordOccurrence(sourceType, sourceLabel, userAnswer, answeredAt);
    }

    private Map<Long, QuestionMeta> loadQuestionMetaMap(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = userMistakeQueryRepository.findQuestionMetaByIdIn(questionIds);
        Map<Long, QuestionMeta> metaMap = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            Long qId = toLong(row[0]);
            if (qId == null) {
                continue;
            }
            Long partId = toLong(row[1]);
            String partName = toStringOrNull(row[2]);
            String questionName = toStringOrNull(row[3]);
            metaMap.put(qId, new QuestionMeta(qId, partId, partName, questionName));
        }
        return metaMap;
    }

    private Map<Long, List<AbilityTag>> loadAbilityTagsMap(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionAbility> links = questionAbilityRepository.findWithAbilityByQuestionIdIn(questionIds);
        Map<Long, List<AbilityTag>> map = new HashMap<>();
        for (QuestionAbility qa : links) {
            if (qa.getQuestion() == null || qa.getAbility() == null) {
                continue;
            }
            Ability ability = qa.getAbility();
            map.computeIfAbsent(qa.getQuestion().getId(), ignored -> new ArrayList<>())
                    .add(AbilityTag.builder()
                            .abilityId(ability.getId())
                            .abilityName(ability.getName())
                            .build());
        }
        return map;
    }

    /**
     * Dựng danh sách tùy chọn bộ lọc (Part, Ability, Nguồn) kèm số lượng câu sai thực tế của học viên.
     */
    private FilterOptions buildFilterOptions(
            List<AggregatedMistake> allMistakes,
            Map<Long, QuestionMeta> metaByQuestionId,
            Map<Long, List<AbilityTag>> abilitiesByQuestionId
    ) {
        Map<Long, PartCounter> partCounters = new HashMap<>();
        Map<Long, AbilityCounter> abilityCounters = new HashMap<>();
        Map<EMistakeSourceType, Integer> sourceCounters = new EnumMap<>(EMistakeSourceType.class);

        for (AggregatedMistake mistake : allMistakes) {
            QuestionMeta meta = metaByQuestionId.get(mistake.questionId());
            Long partId = meta != null ? meta.partId() : null;
            if (partId != null) {
                String partName = meta.partName() != null ? meta.partName() : "Part " + partId;
                partCounters.computeIfAbsent(partId, id -> new PartCounter(id, partName)).increment();
            }

            List<AbilityTag> tags = abilitiesByQuestionId.getOrDefault(mistake.questionId(), Collections.emptyList());
            for (AbilityTag tag : tags) {
                if (tag.getAbilityId() == null) {
                    continue;
                }
                abilityCounters.computeIfAbsent(
                        tag.getAbilityId(),
                        id -> new AbilityCounter(id, tag.getAbilityName(), partId)
                ).increment();
            }

            // Đếm mỗi nguồn mà câu hỏi từng bị làm sai (mỗi câu tính tối đa 1 lần cho mỗi nguồn)
            for (EMistakeSourceType src : mistake.sourceTypes()) {
                sourceCounters.merge(src, 1, Integer::sum);
            }
        }

        List<PartCountItem> parts = partCounters.values().stream()
                .sorted(Comparator.comparing(PartCounter::partId))
                .map(c -> PartCountItem.builder()
                        .partId(c.partId())
                        .partName(c.partName())
                        .count(c.count())
                        .build())
                .toList();

        List<AbilityCountItem> abilities = abilityCounters.values().stream()
                .sorted(Comparator
                        .comparing(AbilityCounter::partId, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AbilityCounter::abilityName, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> AbilityCountItem.builder()
                        .abilityId(c.abilityId())
                        .abilityName(c.abilityName())
                        .partId(c.partId())
                        .count(c.count())
                        .build())
                .toList();

        List<SourceCountItem> sources = new ArrayList<>();
        for (EMistakeSourceType type : EMistakeSourceType.values()) {
            Integer count = sourceCounters.get(type);
            if (count != null && count > 0) {
                sources.add(SourceCountItem.builder()
                        .sourceType(type)
                        .label(userMistakeNotebookMapper.resolveSourceTypeDisplayLabel(type))
                        .count(count)
                        .build());
            }
        }

        return FilterOptions.builder()
                .parts(parts)
                .abilities(abilities)
                .sources(sources)
                .build();
    }

    /**
     * Lọc danh sách câu sai theo đồng thời Part, Ability, Nguồn bài thi và Từ khóa tìm kiếm.
     */
    private List<AggregatedMistake> filterMistakes(
            List<AggregatedMistake> allMistakes,
            Map<Long, QuestionMeta> metaByQuestionId,
            Map<Long, List<AbilityTag>> abilitiesByQuestionId,
            Long partId,
            Long abilityId,
            EMistakeSourceType sourceType,
            String keyword
    ) {
        String normalizedKeyword = keyword != null && !keyword.isBlank()
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        List<AggregatedMistake> result = new ArrayList<>();
        for (AggregatedMistake mistake : allMistakes) {
            QuestionMeta meta = metaByQuestionId.get(mistake.questionId());
            // Bỏ qua nếu câu hỏi gốc không còn tồn tại trong bảng questions
            if (meta == null) {
                continue;
            }
            if (partId != null && !Objects.equals(meta.partId(), partId)) {
                continue;
            }
            if (sourceType != null && !mistake.sourceTypes().contains(sourceType)) {
                continue;
            }
            if (abilityId != null) {
                List<AbilityTag> tags = abilitiesByQuestionId.getOrDefault(
                        mistake.questionId(),
                        Collections.emptyList()
                );
                boolean matchesAbility = tags.stream().anyMatch(tag -> Objects.equals(tag.getAbilityId(), abilityId));
                if (!matchesAbility) {
                    continue;
                }
            }
            if (normalizedKeyword != null) {
                boolean matchesName = meta.questionName() != null
                        && meta.questionName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
                boolean matchesSource = mistake.latestSourceLabel() != null
                        && mistake.latestSourceLabel().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
                boolean matchesPart = meta.partName() != null
                        && meta.partName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
                boolean matchesAbilityName = abilitiesByQuestionId
                        .getOrDefault(mistake.questionId(), Collections.emptyList())
                        .stream()
                        .anyMatch(tag -> tag.getAbilityName() != null
                                && tag.getAbilityName().toLowerCase(Locale.ROOT).contains(normalizedKeyword));
                if (!matchesName && !matchesSource && !matchesPart && !matchesAbilityName) {
                    continue;
                }
            }
            result.add(mistake);
        }
        return result;
    }

    private MistakeNotebookResponse buildEmptyNotebookResponse(int page, int size) {
        return MistakeNotebookResponse.builder()
                .totalWrongCount(0)
                .filterOptions(FilterOptions.builder()
                        .parts(Collections.emptyList())
                        .abilities(Collections.emptyList())
                        .sources(Collections.emptyList())
                        .build())
                .items(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String toStringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        return value instanceof LocalDateTime ldt ? ldt : null;
    }

    private static final class OutputAttemptSectionCounter {
        private int listeningTotal;
        private int listeningCorrect;
        private int readingTotal;
        private int readingCorrect;
    }

    private record QuestionMeta(Long questionId, Long partId, String partName, String questionName) {
    }

    private record AggregatedMistake(
            Long questionId,
            EMistakeSourceType latestSourceType,
            String latestSourceLabel,
            String lastUserAnswer,
            LocalDateTime lastWrongAt,
            int wrongCount,
            Set<EMistakeSourceType> sourceTypes
    ) {
    }

    private static final class MutableMistakeAccumulator {
        private final Long questionId;
        private EMistakeSourceType latestSourceType;
        private String latestSourceLabel;
        private String lastUserAnswer;
        private LocalDateTime lastWrongAt;
        private int wrongCount;
        private final EnumSet<EMistakeSourceType> sourceTypes;

        private MutableMistakeAccumulator(
                Long questionId,
                EMistakeSourceType sourceType,
                String sourceLabel,
                String userAnswer,
                LocalDateTime answeredAt
        ) {
            this.questionId = questionId;
            this.latestSourceType = sourceType;
            this.latestSourceLabel = sourceLabel;
            this.lastUserAnswer = userAnswer;
            this.lastWrongAt = answeredAt;
            this.wrongCount = 1;
            this.sourceTypes = EnumSet.of(sourceType);
        }

        private void recordOccurrence(
                EMistakeSourceType sourceType,
                String sourceLabel,
                String userAnswer,
                LocalDateTime answeredAt
        ) {
            this.wrongCount++;
            this.sourceTypes.add(sourceType);
            // Cập nhật thông tin nguồn & đáp án chọn sai gần nhất nếu lần sai này có mốc thời gian mới hơn
            if (answeredAt != null && (this.lastWrongAt == null || answeredAt.isAfter(this.lastWrongAt))) {
                this.lastWrongAt = answeredAt;
                this.latestSourceType = sourceType;
                this.latestSourceLabel = sourceLabel;
                this.lastUserAnswer = userAnswer;
            }
        }

        private AggregatedMistake toImmutable() {
            return new AggregatedMistake(
                    questionId,
                    latestSourceType,
                    latestSourceLabel,
                    lastUserAnswer,
                    lastWrongAt,
                    wrongCount,
                    Collections.unmodifiableSet(sourceTypes)
            );
        }
    }

    private static final class PartCounter {
        private final Long partId;
        private final String partName;
        private int count;

        private PartCounter(Long partId, String partName) {
            this.partId = partId;
            this.partName = partName;
        }

        private void increment() {
            this.count++;
        }

        private Long partId() {
            return partId;
        }

        private String partName() {
            return partName;
        }

        private int count() {
            return count;
        }
    }

    private static final class AbilityCounter {
        private final Long abilityId;
        private final String abilityName;
        private final Long partId;
        private int count;

        private AbilityCounter(Long abilityId, String abilityName, Long partId) {
            this.abilityId = abilityId;
            this.abilityName = abilityName;
            this.partId = partId;
        }

        private void increment() {
            this.count++;
        }

        private Long abilityId() {
            return abilityId;
        }

        private String abilityName() {
            return abilityName;
        }

        private Long partId() {
            return partId;
        }

        private int count() {
            return count;
        }
    }
}
