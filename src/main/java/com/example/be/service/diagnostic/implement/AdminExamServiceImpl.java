package com.example.be.service.diagnostic.implement;

import com.example.be.dto.request.diagnostic.AutoSelectQuestionsRequest;
import com.example.be.dto.request.diagnostic.CreateExamRequest;
import com.example.be.dto.request.diagnostic.UpdateExamRequest;
import com.example.be.dto.response.diagnostic.AutoSelectQuestionsResponse;
import com.example.be.dto.response.diagnostic.ExamDetailResponse;
import com.example.be.dto.response.diagnostic.ExamListItemResponse;
import com.example.be.dto.response.diagnostic.ExamMatrixAvailabilityResponse;
import com.example.be.dto.response.diagnostic.ExamQuestionItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.entity.setting.AbilityEvaluationRule;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.EExamTag;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.diagnostic.ExamMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.repository.setting.AbilityEvaluationRuleRepository;
import com.example.be.service.diagnostic.service.AdminExamService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý đề thi (Diagnostic Test & Mock Test).
 */
@Service
@RequiredArgsConstructor
public class AdminExamServiceImpl implements AdminExamService {

    private static final int DEFAULT_POINTS_PER_QUESTION = 5;
    private static final int FULL_MOCK_TOTAL_QUESTIONS = 200;
    private static final int DEFAULT_DIAGNOSTIC_TOTAL_QUESTIONS = 48;
    private static final int DEFAULT_MIN_EVIDENCE_COUNT = 3;
    private static final int TOTAL_TOEIC_PARTS = 7;
    private static final Map<Integer, Integer> ETS_STANDARD_MATRIX = Map.of(
            1, 6,
            2, 25,
            3, 39,
            4, 30,
            5, 30,
            6, 16,
            7, 54
    );

    private final DiagnosticTestRepository diagnosticTestRepository;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final QuestionRepository questionRepository;
    private final AbilityRepository abilityRepository;
    private final QuestionAbilityRepository questionAbilityRepository;
    private final AbilityEvaluationRuleRepository abilityEvaluationRuleRepository;
    private final ExamMapper examMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ExamListItemResponse> getExams(String keyword, ETestType testType, EExamStatus examStatus, Pageable pageable) {
        Specification<DiagnosticTest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }

            if (testType != null) {
                predicates.add(cb.equal(root.get("testType"), testType));
            }

            if (examStatus != null) {
                predicates.add(cb.equal(root.get("examStatus"), examStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return diagnosticTestRepository.findAll(spec, pageable).map(examMapper::toListItemResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getExamTabCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("all", diagnosticTestRepository.count());
        counts.put("diagnostic", diagnosticTestRepository.countByTestType(ETestType.DIAGNOSTIC));
        counts.put("mock", diagnosticTestRepository.countByTestType(ETestType.FULL_MOCK_TEST));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetailResponse getExamById(Long id) {
        DiagnosticTest test = diagnosticTestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy đề thi ID: " + id));

        Map<Long, List<String>> questionAbilityMap = loadQuestionAbilityMap(test);
        return examMapper.toDetailResponse(test, questionAbilityMap);
    }

    @Override
    @Transactional
    public ExamDetailResponse createExam(CreateExamRequest request, String currentUsername) {
        // Bước 1: Xác định trạng thái ban đầu theo yêu cầu nghiệp vụ
        // isDraft = true -> DRAFT; isDraft = false -> ARCHIVE (sẵn sàng, Admin sẽ kích hoạt sau)
        boolean isDraft = Boolean.TRUE.equals(request.getIsDraft());
        EExamStatus initialStatus = isDraft ? EExamStatus.DRAFT : EExamStatus.ARCHIVE;

        // Bước 2: Kiểm tra tính hợp lệ của danh sách câu hỏi nếu đề được lưu hoàn tất (không phải nháp)
        List<Long> questionIds = request.getQuestionIds() != null ? request.getQuestionIds() : Collections.emptyList();
        if (!isDraft) {
            validateExamQuestionsForSave(request.getTestType(), questionIds);
        }

        // Bước 3: Khởi tạo Entity DiagnosticTest qua ExamMapper
        DiagnosticTest exam = examMapper.toEntity(request, currentUsername, initialStatus);

        // Bước 4: Ánh xạ danh sách câu hỏi vào bảng trung gian diagnostic_test_questions
        if (!questionIds.isEmpty()) {
            attachQuestionsToExam(exam, questionIds);
        }

        // Bước 5: Lưu vào cơ sở dữ liệu và trả về DTO chi tiết
        DiagnosticTest saved = diagnosticTestRepository.save(exam);
        Map<Long, List<String>> questionAbilityMap = loadQuestionAbilityMap(saved);
        return examMapper.toDetailResponse(saved, questionAbilityMap);
    }

    @Override
    @Transactional
    public ExamDetailResponse updateExam(Long id, UpdateExamRequest request, String currentUsername) {
        // Bước 1: Kiểm tra sự tồn tại của đề thi
        DiagnosticTest exam = diagnosticTestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy đề thi ID: " + id));

        // Bước 2: Kiểm tra xem đề thi đã từng có học viên làm bài chưa
        boolean hasAttempts = diagnosticAttemptRepository.existsByDiagnosticTestId(id);
        List<Long> newQuestionIds = request.getQuestionIds() != null ? request.getQuestionIds() : Collections.emptyList();

        // Nếu đã có lượt làm bài mà câu hỏi bị thay đổi -> Chặn tuyệt đối để đảm bảo toàn vẹn dữ liệu khảo thí
        if (hasAttempts) {
            List<Long> currentQuestionIds = exam.getTestQuestions().stream()
                    .map(tq -> tq.getQuestion().getId())
                    .toList();
            if (!currentQuestionIds.equals(newQuestionIds)) {
                throw new BaseException(ErrorCode.EXAM_IN_USE, "Đề thi đã có lượt làm bài, không thể thay đổi danh sách câu hỏi");
            }
        }

        // Bước 3: Kiểm tra hợp lệ nếu chuyển từ DRAFT sang Lưu hoàn tất (ARCHIVE)
        boolean isDraft = Boolean.TRUE.equals(request.getIsDraft());
        if (!isDraft) {
            validateExamQuestionsForSave(request.getTestType(), newQuestionIds);
            if (exam.getExamStatus() == EExamStatus.DRAFT) {
                exam.setExamStatus(EExamStatus.ARCHIVE);
            }
        } else {
            exam.setExamStatus(EExamStatus.DRAFT);
        }

        // Bước 4: Cập nhật thông tin cơ bản
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTestType(request.getTestType());
        if (request.getTestType() == ETestType.FULL_MOCK_TEST) {
            if (request.getExamTag() != null) {
                exam.setExamTag(request.getExamTag());
            }
        } else {
            exam.setExamTag(EExamTag.PRACTICE);
        }
        exam.setUpdatedBy(currentUsername != null ? currentUsername : "Admin");

        // Bước 5: Cập nhật danh sách câu hỏi nếu chưa có attempt
        if (!hasAttempts) {
            exam.getTestQuestions().clear();
            attachQuestionsToExam(exam, newQuestionIds);
        }

        DiagnosticTest updated = diagnosticTestRepository.save(exam);
        Map<Long, List<String>> questionAbilityMap = loadQuestionAbilityMap(updated);
        return examMapper.toDetailResponse(updated, questionAbilityMap);
    }

    @Override
    @Transactional
    public void activateExam(Long id, String currentUsername) {
        // Bước 1: Tìm đề thi
        DiagnosticTest exam = diagnosticTestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy đề thi ID: " + id));

        // Bước 2: Kiểm tra trạng thái hiện tại
        if (exam.getExamStatus() == EExamStatus.ACTIVE) {
            throw new BaseException(ErrorCode.EXAM_ALREADY_ACTIVE);
        }

        // Bước 3: QUY TẮC ĐẶC THÙ ĐỀ CHẨN ĐOÁN
        // Chỉ được có duy nhất 1 đề Diagnostic ở trạng thái ACTIVE tại một thời điểm
        if (exam.getTestType() == ETestType.DIAGNOSTIC) {
            Optional<DiagnosticTest> currentActive = diagnosticTestRepository.findFirstByTestTypeAndExamStatus(
                    ETestType.DIAGNOSTIC,
                    EExamStatus.ACTIVE
            );

            if (currentActive.isPresent() && !currentActive.get().getId().equals(id)) {
                DiagnosticTest oldActive = currentActive.get();
                oldActive.setExamStatus(EExamStatus.ARCHIVE);
                oldActive.setUpdatedBy(currentUsername);
                diagnosticTestRepository.save(oldActive);
            }
        }

        // Bước 4: Đánh dấu đề mới là ACTIVE
        exam.setExamStatus(EExamStatus.ACTIVE);
        exam.setUpdatedBy(currentUsername != null ? currentUsername : "Admin");
        diagnosticTestRepository.save(exam);
    }

    @Override
    @Transactional
    public void archiveExam(Long id, String currentUsername) {
        DiagnosticTest exam = diagnosticTestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy đề thi ID: " + id));

        exam.setExamStatus(EExamStatus.ARCHIVE);
        exam.setUpdatedBy(currentUsername != null ? currentUsername : "Admin");
        diagnosticTestRepository.save(exam);
    }

    @Override
    @Transactional
    public void deleteExam(Long id) {
        // Bước 1: Tìm đề thi
        DiagnosticTest exam = diagnosticTestRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.EXAM_NOT_FOUND, "Không tìm thấy đề thi ID: " + id));

        // Bước 2: Kiểm tra toàn vẹn dữ liệu
        // Nếu đề thi đã có bất kỳ lượt làm bài nào (kể cả đã nộp hay đang làm), CẤM XÓA VẬT LÝ để bảo vệ dữ liệu lịch sử
        if (diagnosticAttemptRepository.existsByDiagnosticTestId(id)) {
            throw new BaseException(ErrorCode.EXAM_IN_USE, "Đề thi đã có học viên tham gia làm bài, không thể xóa. Vui lòng chuyển sang Lưu trữ (Archive).");
        }

        // Bước 3: Xóa an toàn
        diagnosticTestRepository.delete(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamMatrixAvailabilityResponse getMatrixAvailability() {
        Map<Integer, Integer> partCounts = new HashMap<>();
        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
            long count = questionRepository.countPlayableQuestionsByPartId((long) partNo);
            partCounts.put(partNo, (int) count);
        }

        // Nạp ngưỡng số câu tối thiểu của từng Ability từ bảng ability_evaluation_rules
        Map<Long, Integer> minEvidenceByAbilityId = loadMinEvidenceByAbilityId();

        // Lấy danh sách toàn bộ Abilities trong DB kèm số câu hỏi và số câu tối thiểu tương ứng
        List<Ability> allAbilities = abilityRepository.findAll();
        List<ExamMatrixAvailabilityResponse.AbilityAvailabilityItem> abilityItems = allAbilities.stream()
                .map(ab -> {
                    long qCount = questionAbilityRepository.countByAbilityId(ab.getId());
                    int minEvidence = minEvidenceByAbilityId.getOrDefault(ab.getId(), DEFAULT_MIN_EVIDENCE_COUNT);
                    return ExamMatrixAvailabilityResponse.AbilityAvailabilityItem.builder()
                            .id(ab.getId())
                            .name(ab.getName())
                            .section(ab.getSections() != null ? ab.getSections().name() : null)
                            .partNumber(ab.getPart() != null && ab.getPart().getId() != null ? ab.getPart().getId().intValue() : null)
                            .availableQuestionCount((int) qCount)
                            .minEvidenceCount(minEvidence)
                            .build();
                })
                .toList();

        return ExamMatrixAvailabilityResponse.builder()
                .partAvailableCounts(partCounts)
                .singlePassageCount(partCounts.getOrDefault(7, 0) / 2)
                .multiplePassageCount(partCounts.getOrDefault(7, 0) - (partCounts.getOrDefault(7, 0) / 2))
                .abilities(abilityItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AutoSelectQuestionsResponse autoSelectQuestions(AutoSelectQuestionsRequest request) {
        List<Long> selectedIds = request.getTestType() == ETestType.FULL_MOCK_TEST
                ? selectQuestionsForMockTest(request)
                : selectQuestionsForDiagnosticTest(request);

        List<Question> pickedQuestions = selectedIds.isEmpty()
                ? Collections.emptyList()
                : questionRepository.findAllWithAbilitiesByIdIn(selectedIds);

        // Sắp xếp câu hỏi theo thứ tự Part 1 -> Part 7, sau đó theo ID để hiển thị mạch lạc
        List<Question> sortedQuestions = pickedQuestions.stream()
                .sorted(Comparator
                        .comparingInt((Question q) -> (q.getPart() != null && q.getPart().getId() != null)
                                ? q.getPart().getId().intValue()
                                : TOTAL_TOEIC_PARTS)
                        .thenComparing(Question::getId))
                .toList();

        List<ExamQuestionItemResponse> questionResponses = examMapper.toPlayableQuestionItemResponses(sortedQuestions);
        List<Long> orderedIds = sortedQuestions.stream().map(Question::getId).toList();

        return AutoSelectQuestionsResponse.builder()
                .totalSelected(orderedIds.size())
                .questionIds(orderedIds)
                .questions(questionResponses)
                .build();
    }

    private List<Long> selectQuestionsForMockTest(AutoSelectQuestionsRequest request) {
        List<Long> selectedIds = new ArrayList<>();
        Map<Integer, Integer> targetMatrix = request.getCustomDistribution() != null && !request.getCustomDistribution().isEmpty()
                ? request.getCustomDistribution()
                : ETS_STANDARD_MATRIX;

        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
            int requiredCount = targetMatrix.getOrDefault(partNo, 0);
            if (requiredCount <= 0) {
                continue;
            }

            List<Question> available = questionRepository.findPlayableQuestionsByPartId((long) partNo);
            if (available.size() < requiredCount) {
                throw new BaseException(ErrorCode.QUESTION_BANK_NOT_ENOUGH,
                        String.format("Part %d không đủ câu hỏi: Cần %d câu nhưng chỉ có %d câu", partNo, requiredCount, available.size()));
            }

            List<Question> shuffled = new ArrayList<>(available);
            Collections.shuffle(shuffled);
            for (int i = 0; i < requiredCount; i++) {
                selectedIds.add(shuffled.get(i).getId());
            }
        }
        return selectedIds;
    }

    /**
     * Tự động chọn câu hỏi cho Đề thi chẩn đoán:
     * 1. Đảm bảo mỗi Ability đạt tối thiểu minEvidenceCount câu từ ability_evaluation_rules (mặc định 48 câu).
     * 2. Phân bổ phần câu bổ sung đều/theo tỷ lệ 7 Part cho đến khi đạt targetTotal.
     */
    private List<Long> selectQuestionsForDiagnosticTest(AutoSelectQuestionsRequest request) {
        List<Ability> allAbilities = abilityRepository.findAll();
        Map<Long, Integer> minEvidenceByAbilityId = loadMinEvidenceByAbilityId();

        int minRequiredTotal = allAbilities.stream()
                .mapToInt(ab -> minEvidenceByAbilityId.getOrDefault(ab.getId(), DEFAULT_MIN_EVIDENCE_COUNT))
                .sum();
        int defaultTotal = minRequiredTotal > 0 ? minRequiredTotal : DEFAULT_DIAGNOSTIC_TOTAL_QUESTIONS;
        int targetTotal = request.getTotalQuestions() != null && request.getTotalQuestions() > 0
                ? request.getTotalQuestions()
                : defaultTotal;

        List<Question> playableQuestions = questionRepository.findAllPlayableWithAbilitiesAndPart();
        Map<Long, List<Question>> playableByAbilityId = new HashMap<>();
        Map<Integer, List<Question>> playableByPart = new HashMap<>();

        for (Question q : playableQuestions) {
            int partNo = (q.getPart() != null && q.getPart().getId() != null) ? q.getPart().getId().intValue() : 1;
            playableByPart.computeIfAbsent(partNo, k -> new ArrayList<>()).add(q);

            if (q.getQuestionAbilities() != null) {
                for (QuestionAbility qa : q.getQuestionAbilities()) {
                    if (qa.getAbility() != null) {
                        playableByAbilityId.computeIfAbsent(qa.getAbility().getId(), k -> new ArrayList<>()).add(q);
                    }
                }
            }
        }

        Set<Long> pickedIds = new LinkedHashSet<>();
        Map<Long, Integer> coveredCountByAbilityId = new HashMap<>();
        Map<Integer, Integer> pickedCountByPart = new HashMap<>();

        // Bước 1: Phủ tối thiểu theo Quy tắc Đánh giá Năng lực (nếu targetTotal đủ cho mức sàn; nếu nhỏ hơn thì phủ 1 câu/Ability trước)
        boolean enforceFullMinEvidence = targetTotal >= minRequiredTotal;
        for (Ability ability : allAbilities) {
            int required = enforceFullMinEvidence
                    ? minEvidenceByAbilityId.getOrDefault(ability.getId(), DEFAULT_MIN_EVIDENCE_COUNT)
                    : 1;
            int alreadyCovered = coveredCountByAbilityId.getOrDefault(ability.getId(), 0);
            if (alreadyCovered >= required) {
                continue;
            }

            List<Question> candidates = new ArrayList<>(playableByAbilityId.getOrDefault(ability.getId(), Collections.emptyList()));
            Collections.shuffle(candidates);

            for (Question candidate : candidates) {
                if (coveredCountByAbilityId.getOrDefault(ability.getId(), 0) >= required) {
                    break;
                }
                if (!enforceFullMinEvidence && pickedIds.size() >= targetTotal) {
                    break;
                }
                if (!pickedIds.contains(candidate.getId())) {
                    markQuestionPicked(candidate, pickedIds, coveredCountByAbilityId, pickedCountByPart);
                }
            }

            int afterPick = coveredCountByAbilityId.getOrDefault(ability.getId(), 0);
            if (enforceFullMinEvidence && afterPick < required) {
                int partNo = (ability.getPart() != null && ability.getPart().getId() != null)
                        ? ability.getPart().getId().intValue()
                        : 1;
                throw new BaseException(ErrorCode.QUESTION_BANK_NOT_ENOUGH,
                        String.format("Kỹ năng '%s' (Part %d) không đủ câu hỏi: Cần tối thiểu %d câu nhưng chỉ có %d câu",
                                ability.getName(), partNo, required, afterPick));
            }
        }

        // Bước 2: Đảm bảo đủ cả 7 Part mỗi Part có ít nhất 1 câu (trường hợp có Part chưa gắn Ability)
        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS && pickedIds.size() < targetTotal; partNo++) {
            if (pickedCountByPart.getOrDefault(partNo, 0) > 0) {
                continue;
            }
            List<Question> partCandidates = new ArrayList<>(playableByPart.getOrDefault(partNo, Collections.emptyList()));
            Collections.shuffle(partCandidates);
            for (Question candidate : partCandidates) {
                if (!pickedIds.contains(candidate.getId())) {
                    markQuestionPicked(candidate, pickedIds, coveredCountByAbilityId, pickedCountByPart);
                    break;
                }
            }
        }

        // Bước 3: Bổ sung các câu còn thiếu theo phân bổ 7 Part (ưu tiên customDistribution nếu có, ngược lại theo tỷ lệ ETS)
        if (pickedIds.size() < targetTotal) {
            Map<Integer, Integer> partTargets = resolveDiagnosticPartTargets(
                    targetTotal,
                    pickedCountByPart,
                    request.getCustomDistribution()
            );

            for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS && pickedIds.size() < targetTotal; partNo++) {
                int partTarget = partTargets.getOrDefault(partNo, 0);
                if (pickedCountByPart.getOrDefault(partNo, 0) >= partTarget) {
                    continue;
                }

                List<Question> partCandidates = new ArrayList<>(playableByPart.getOrDefault(partNo, Collections.emptyList()));
                Collections.shuffle(partCandidates);
                for (Question candidate : partCandidates) {
                    if (pickedCountByPart.getOrDefault(partNo, 0) >= partTarget || pickedIds.size() >= targetTotal) {
                        break;
                    }
                    if (!pickedIds.contains(candidate.getId())) {
                        markQuestionPicked(candidate, pickedIds, coveredCountByAbilityId, pickedCountByPart);
                    }
                }
            }

            // Fallback vòng tròn qua 7 Part nếu Part nào đó trong ngân hàng không đủ chỉ tiêu phụ
            boolean addedInRound = true;
            Map<Integer, List<Question>> shuffledRemainingByPart = new HashMap<>();
            for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
                List<Question> remaining = playableByPart.getOrDefault(partNo, Collections.emptyList()).stream()
                        .filter(q -> !pickedIds.contains(q.getId()))
                        .collect(Collectors.toCollection(ArrayList::new));
                Collections.shuffle(remaining);
                shuffledRemainingByPart.put(partNo, remaining);
            }

            while (pickedIds.size() < targetTotal && addedInRound) {
                addedInRound = false;
                for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS && pickedIds.size() < targetTotal; partNo++) {
                    List<Question> pool = shuffledRemainingByPart.get(partNo);
                    if (pool != null && !pool.isEmpty()) {
                        Question candidate = pool.remove(0);
                        markQuestionPicked(candidate, pickedIds, coveredCountByAbilityId, pickedCountByPart);
                        addedInRound = true;
                    }
                }
            }
        }

        return new ArrayList<>(pickedIds);
    }

    private void markQuestionPicked(
            Question candidate,
            Set<Long> pickedIds,
            Map<Long, Integer> coveredCountByAbilityId,
            Map<Integer, Integer> pickedCountByPart
    ) {
        pickedIds.add(candidate.getId());
        int partNo = (candidate.getPart() != null && candidate.getPart().getId() != null)
                ? candidate.getPart().getId().intValue()
                : 1;
        pickedCountByPart.merge(partNo, 1, Integer::sum);

        if (candidate.getQuestionAbilities() != null) {
            for (QuestionAbility qa : candidate.getQuestionAbilities()) {
                if (qa.getAbility() != null) {
                    coveredCountByAbilityId.merge(qa.getAbility().getId(), 1, Integer::sum);
                }
            }
        }
    }

    private Map<Integer, Integer> resolveDiagnosticPartTargets(
            int targetTotal,
            Map<Integer, Integer> currentByPart,
            Map<Integer, Integer> customDistribution
    ) {
        if (customDistribution != null && !customDistribution.isEmpty()) {
            return customDistribution;
        }

        int currentSum = currentByPart.values().stream().mapToInt(Integer::intValue).sum();
        int extraNeeded = Math.max(0, targetTotal - currentSum);
        Map<Integer, Integer> targets = new HashMap<>();
        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
            targets.put(partNo, currentByPart.getOrDefault(partNo, 0));
        }
        if (extraNeeded == 0) {
            return targets;
        }

        Map<Integer, Double> remainders = new HashMap<>();
        int allocatedExtra = 0;
        for (int partNo = 1; partNo <= TOTAL_TOEIC_PARTS; partNo++) {
            double exactShare = (extraNeeded * (double) ETS_STANDARD_MATRIX.getOrDefault(partNo, 0)) / FULL_MOCK_TOTAL_QUESTIONS;
            int floorShare = (int) Math.floor(exactShare);
            targets.merge(partNo, floorShare, Integer::sum);
            remainders.put(partNo, exactShare - floorShare);
            allocatedExtra += floorShare;
        }

        int leftover = extraNeeded - allocatedExtra;
        List<Integer> partsByRemainder = remainders.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()
                        .thenComparingInt(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();

        for (int i = 0; i < leftover; i++) {
            int partNo = partsByRemainder.get(i % partsByRemainder.size());
            targets.merge(partNo, 1, Integer::sum);
        }

        return targets;
    }

    private Map<Long, Integer> loadMinEvidenceByAbilityId() {
        List<AbilityEvaluationRule> rules = abilityEvaluationRuleRepository.findAllWithDetails();
        Map<Long, Integer> map = new HashMap<>();
        for (AbilityEvaluationRule rule : rules) {
            if (rule.getAbility() != null && rule.getMinEvidenceCount() != null && rule.getMinEvidenceCount() > 0) {
                map.put(rule.getAbility().getId(), rule.getMinEvidenceCount());
            }
        }
        return map;
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Nạp trước Map QuestionId -> AbilityNames để phục vụ Mapper triệt tiêu lỗi N+1 Query.
     */
    private Map<Long, List<String>> loadQuestionAbilityMap(DiagnosticTest exam) {
        if (exam.getTestQuestions() == null || exam.getTestQuestions().isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> questionIds = exam.getTestQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .toList();

        Map<Long, List<String>> questionAbilityMap = new HashMap<>();
        List<QuestionAbility> questionAbilities = questionAbilityRepository.findWithAbilityByQuestionIdIn(questionIds);
        for (QuestionAbility qa : questionAbilities) {
            questionAbilityMap.computeIfAbsent(qa.getQuestion().getId(), k -> new ArrayList<>())
                    .add(qa.getAbility().getName());
        }
        return questionAbilityMap;
    }

    /**
     * Gắn danh sách ID câu hỏi vào đề thi kèm thứ tự orderNo.
     */
    private void attachQuestionsToExam(DiagnosticTest exam, List<Long> questionIds) {
        List<Question> questions = questionRepository.findAllById(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int order = 1;
        for (Long qId : questionIds) {
            Question q = questionMap.get(qId);
            if (q != null) {
                DiagnosticTestQuestion dtq = new DiagnosticTestQuestion();
                dtq.setDiagnosticTest(exam);
                dtq.setQuestion(q);
                dtq.setOrderNo(order++);
                dtq.setPoints(DEFAULT_POINTS_PER_QUESTION);
                exam.getTestQuestions().add(dtq);
            }
        }
    }

    /**
     * Kiểm tra tính hợp lệ của câu hỏi khi lưu đề thi chính thức (không phải nháp).
     */
    private void validateExamQuestionsForSave(ETestType testType, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            throw new BaseException(ErrorCode.EXAM_INVALID_MATRIX, "Đề thi chính thức bắt buộc phải có câu hỏi");
        }

        if (testType == ETestType.DIAGNOSTIC) {
            // QUY TẮC NGHIỆP VỤ: Đề chẩn đoán phải phủ đủ toàn bộ Ability trong database
            List<Ability> allAbilities = abilityRepository.findAll();
            if (!allAbilities.isEmpty()) {
                List<Long> coveredAbilityIds = questionAbilityRepository.findDistinctAbilityIdsByQuestionIdIn(questionIds);
                if (coveredAbilityIds.size() < allAbilities.size()) {
                    throw new BaseException(ErrorCode.DIAGNOSTIC_MISSING_ABILITIES,
                            String.format("Đề thi chẩn đoán chưa phủ đủ toàn bộ kỹ năng: Chỉ đạt %d/%d Abilities", coveredAbilityIds.size(), allAbilities.size()));
                }
            }
        } else if (testType == ETestType.FULL_MOCK_TEST) {
            if (questionIds.size() != FULL_MOCK_TOTAL_QUESTIONS) {
                throw new BaseException(ErrorCode.EXAM_INVALID_MATRIX,
                        String.format("Đề thi thử TOEIC chuẩn ETS bắt buộc phải có đủ 200 câu hỏi (hiện có: %d câu)", questionIds.size()));
            }
        }
    }
}
