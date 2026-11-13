package com.example.be.service.exam.implement;

import com.example.be.dto.request.exam.AutoSelectQuestionsRequest;
import com.example.be.dto.request.exam.CreateExamRequest;
import com.example.be.dto.request.exam.UpdateExamRequest;
import com.example.be.dto.response.exam.AutoSelectQuestionsResponse;
import com.example.be.dto.response.exam.ExamDetailResponse;
import com.example.be.dto.response.exam.ExamListItemResponse;
import com.example.be.dto.response.exam.ExamMatrixAvailabilityResponse;
import com.example.be.dto.response.exam.ExamQuestionItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.diagnostic.DiagnosticTest;
import com.example.be.entity.question.DiagnosticTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.enums.diagnostic.EExamStatus;
import com.example.be.enums.diagnostic.ETestType;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.exam.ExamMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.diagnostic.DiagnosticAttemptRepository;
import com.example.be.repository.diagnostic.DiagnosticTestRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.exam.service.AdminExamService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private final DiagnosticTestRepository diagnosticTestRepository;
    private final DiagnosticAttemptRepository diagnosticAttemptRepository;
    private final QuestionRepository questionRepository;
    private final AbilityRepository abilityRepository;
    private final QuestionAbilityRepository questionAbilityRepository;
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
        for (int partNo = 1; partNo <= 7; partNo++) {
            long count = questionRepository.countPlayableQuestionsByPartId((long) partNo);
            partCounts.put(partNo, (int) count);
        }

        // Lấy danh sách toàn bộ Abilities trong DB kèm số câu hỏi tương ứng
        List<Ability> allAbilities = abilityRepository.findAll();
        List<ExamMatrixAvailabilityResponse.AbilityAvailabilityItem> abilityItems = allAbilities.stream()
                .map(ab -> {
                    long qCount = questionAbilityRepository.countByAbilityId(ab.getId());
                    return ExamMatrixAvailabilityResponse.AbilityAvailabilityItem.builder()
                            .id(ab.getId())
                            .name(ab.getName())
                            .section(ab.getSections() != null ? ab.getSections().name() : null)
                            .partNumber(ab.getPart() != null && ab.getPart().getId() != null ? ab.getPart().getId().intValue() : null)
                            .availableQuestionCount((int) qCount)
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
        List<Long> selectedIds = new ArrayList<>();

        if (request.getTestType() == ETestType.FULL_MOCK_TEST) {
            // Ma trận chuẩn ETS 200 câu
            Map<Integer, Integer> standardMatrix = Map.of(
                    1, 6,
                    2, 25,
                    3, 39,
                    4, 30,
                    5, 30,
                    6, 16,
                    7, 54
            );

            Map<Integer, Integer> targetMatrix = request.getCustomDistribution() != null && !request.getCustomDistribution().isEmpty()
                    ? request.getCustomDistribution()
                    : standardMatrix;

            for (Map.Entry<Integer, Integer> entry : targetMatrix.entrySet()) {
                int partNo = entry.getKey();
                int requiredCount = entry.getValue();

                List<Question> available = questionRepository.findPlayableQuestionsByPartId((long) partNo);
                if (available.size() < requiredCount) {
                    throw new BaseException(ErrorCode.QUESTION_BANK_NOT_ENOUGH,
                            String.format("Part %d không đủ câu hỏi: Cần %d câu nhưng chỉ có %d câu", partNo, requiredCount, available.size()));
                }

                // Xáo trộn ngẫu nhiên để chọn
                List<Question> shuffled = new ArrayList<>(available);
                Collections.shuffle(shuffled);
                for (int i = 0; i < requiredCount; i++) {
                    selectedIds.add(shuffled.get(i).getId());
                }
            }
        } else {
            // Đề chẩn đoán DIAGNOSTIC (Gợi ý 60 câu)
            // QUY TẮC CỐT LÕI: Phải bao phủ 100% tất cả các Ability có trong database
            List<Ability> allAbilities = abilityRepository.findAll();
            Set<Long> pickedQuestionIds = new HashSet<>();

            // Bước 1: Với mỗi Ability, chọn ít nhất 1 câu hỏi tương ứng
            for (Ability ability : allAbilities) {
                List<Question> questionsWithAbility = questionRepository.findAll().stream()
                        .filter(q -> q.getQuestionAbilities() != null && q.getQuestionAbilities().stream()
                                .anyMatch(qa -> qa.getAbility().getId().equals(ability.getId())))
                        .toList();

                if (!questionsWithAbility.isEmpty()) {
                    List<Question> shuffled = new ArrayList<>(questionsWithAbility);
                    Collections.shuffle(shuffled);
                    pickedQuestionIds.add(shuffled.get(0).getId());
                }
            }

            // Bước 2: Bổ sung thêm các câu hỏi khác cho đủ mục tiêu (mặc định 60 câu hoặc số lượng cấu hình)
            int targetTotal = request.getTotalQuestions() != null && request.getTotalQuestions() > 0
                    ? request.getTotalQuestions()
                    : 60;
            for (int partNo = 1; partNo <= 7 && pickedQuestionIds.size() < targetTotal; partNo++) {
                List<Question> partQuestions = questionRepository.findPlayableQuestionsByPartId((long) partNo);
                List<Question> shuffled = new ArrayList<>(partQuestions);
                Collections.shuffle(shuffled);
                for (Question q : shuffled) {
                    if (pickedQuestionIds.size() >= targetTotal) break;
                    pickedQuestionIds.add(q.getId());
                }
            }

            selectedIds.addAll(pickedQuestionIds);
        }

        // Lấy chi tiết các câu hỏi đã chọn qua ExamMapper
        List<Question> pickedQuestions = questionRepository.findAllById(selectedIds);
        List<ExamQuestionItemResponse> questionResponses = examMapper.toPlayableQuestionItemResponses(pickedQuestions);

        return AutoSelectQuestionsResponse.builder()
                .totalSelected(selectedIds.size())
                .questionIds(selectedIds)
                .questions(questionResponses)
                .build();
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
