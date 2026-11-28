package com.example.be.service.content.implement;

import com.example.be.dto.request.content.AutoSelectModuleTestQuestionsRequest;
import com.example.be.dto.request.content.SaveModuleTestConfigRequest;
import com.example.be.dto.response.content.ModuleTestConfigDetailResponse;
import com.example.be.dto.response.content.ModuleTestOverviewItemResponse;
import com.example.be.dto.response.content.ModuleTestQuestionItemResponse;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.ModuleAbility;
import com.example.be.entity.question.ModuleTestQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionAbility;
import com.example.be.enums.content.EModuleTestSelectionMode;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.ModuleTestMapper;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.content.ModuleTestAnswerRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.question.QuestionAbilityRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.content.service.AdminModuleTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Cấu hình Bài thi vượt ải mô-đun (Use Case A07).
 * Hỗ trợ chọn câu hỏi thủ công (MANUAL) và sinh đề ngẫu nhiên từ kho Ability (RANDOM_BY_ABILITY),
 * tự động bóc tách chùm câu hỏi (Part 3, 4, 6, 7), kiểm tra cảnh báo sư phạm (6b, 6c) và bảo vệ khóa ngoại lịch sử thi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminModuleTestServiceImpl implements AdminModuleTestService {

    private static final int MAX_MODULE_TEST_QUESTIONS = 100;
    private static final int DEFAULT_QUESTION_POINTS = 10;
    private static final int SUMMARY_MAX_LENGTH = 160;

    private final ModuleRepository moduleRepository;
    private final ModuleTestQuestionRepository moduleTestQuestionRepository;
    private final ModuleTestAnswerRepository moduleTestAnswerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAbilityRepository questionAbilityRepository;
    private final ModuleTestMapper moduleTestMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleTestOverviewItemResponse> getAllModuleTestOverviews() {
        // Bước 1: Nạp toàn bộ Module kèm Part và Ability trong 1 câu truy vấn (triệt tiêu N+1)
        List<Module> modules = moduleRepository.findAllWithLessonsAndAbilities();
        if (modules.isEmpty()) {
            return Collections.emptyList();
        }

        // Bước 2: Đếm số câu hỏi bài thi vượt ải theo từng Module bằng 1 câu GROUP BY
        List<Long> moduleIds = modules.stream().map(Module::getId).collect(Collectors.toList());
        Map<Long, Integer> questionCountByModuleId = new HashMap<>();
        List<Object[]> countRows = moduleTestQuestionRepository.countQuestionsGroupedByModuleIds(moduleIds);
        for (Object[] row : countRows) {
            Long modId = (Long) row[0];
            Long count = (Long) row[1];
            questionCountByModuleId.put(modId, count != null ? count.intValue() : 0);
        }

        // Bước 3: Ánh xạ sang danh sách DTO tổng quan (lessons được nạp theo lô qua @BatchSize trên Module)
        return modules.stream()
                .map(module -> moduleTestMapper.toOverviewItem(
                        module,
                        questionCountByModuleId.getOrDefault(module.getId(), 0)
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleTestConfigDetailResponse getModuleTestConfig(Long moduleId) {
        // Bước 1: Tìm Module cần cấu hình
        Module module = findModuleByIdOrThrow(moduleId);
        Set<Long> moduleAbilityIds = extractModuleAbilityIds(module);

        // Bước 2: Đếm số câu hỏi playable khả dụng trong kho khớp Ability của Module (phục vụ hiển thị & kiểm tra 6c)
        long availableCount = countAvailableAbilityQuestions(module, moduleAbilityIds);

        // Bước 3: Nạp danh sách câu hỏi đã gán cho bài thi vượt ải của Module
        List<ModuleTestQuestionItemResponse> questionItems = buildQuestionItems(moduleId, moduleAbilityIds);

        return moduleTestMapper.toConfigDetail(module, questionItems, availableCount);
    }

    @Override
    @Transactional
    public ModuleTestConfigDetailResponse saveModuleTestConfig(Long moduleId, SaveModuleTestConfigRequest request) {
        // Bước 1: Kiểm tra tồn tại Module và validate tham số cơ bản (Ngoại lệ 5a)
        Module module = findModuleByIdOrThrow(moduleId);
        if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0
                || request.getQuestionCount() == null || request.getQuestionCount() <= 0) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Số lượng câu hỏi và thời gian làm bài phải lớn hơn 0"
            );
        }

        Set<Long> moduleAbilityIds = extractModuleAbilityIds(module);
        EModuleTestSelectionMode selectionMode = request.getSelectionMode();

        // Bước 2: Xử lý theo phương thức chọn câu hỏi
        int effectiveQuestionCount = request.getQuestionCount();
        if (selectionMode == EModuleTestSelectionMode.RANDOM_BY_ABILITY) {
            // Luồng thay thế 6a & Ngoại lệ 6c: Kho câu hỏi thuộc Ability của Module phải đủ số câu yêu cầu
            long availableCount = countAvailableAbilityQuestions(module, moduleAbilityIds);
            if (availableCount < request.getQuestionCount()) {
                throw new BaseException(
                        ErrorCode.QUESTION_BANK_NOT_ENOUGH,
                        buildNotEnoughBankMessage(availableCount)
                );
            }
        } else {
            // Luồng chính (MANUAL): Kiểm tra, bóc tách chùm câu hỏi, cảnh báo 6b và đồng bộ chính xác số câu thực tế
            effectiveQuestionCount = syncManualQuestions(module, request, moduleAbilityIds);
        }

        // Bước 3: Cập nhật tham số cấu hình đề vượt ải trên Entity Module
        module.setTestDurationMinutes(request.getDurationMinutes());
        module.setTestQuestionCount(effectiveQuestionCount);
        module.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        module.setTestSelectionMode(selectionMode);
        moduleRepository.save(module);

        log.info("Đã cập nhật cấu hình bài thi vượt ải cho moduleId={} (mode={}, duration={}p, count={})",
                moduleId, selectionMode, request.getDurationMinutes(), effectiveQuestionCount);

        return getModuleTestConfig(moduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleTestQuestionItemResponse> autoSelectQuestions(
            Long moduleId,
            AutoSelectModuleTestQuestionsRequest request
    ) {
        // Bước 1: Kiểm tra tồn tại Module và xác định phạm vi câu hỏi nguồn
        Module module = findModuleByIdOrThrow(moduleId);
        Set<Long> moduleAbilityIds = extractModuleAbilityIds(module);
        boolean onlyMatchingAbilities = !Boolean.FALSE.equals(request.getOnlyMatchingAbilities());

        // Bước 2: Lấy toàn bộ câu hỏi playable khớp Ability (hoặc fallback theo Part nếu Module chưa gắn Ability)
        List<Question> pool = loadQuestionPoolForModule(module, moduleAbilityIds, onlyMatchingAbilities);
        if (pool.size() < request.getQuestionCount()) {
            throw new BaseException(
                    ErrorCode.QUESTION_BANK_NOT_ENOUGH,
                    buildNotEnoughBankMessage(pool.size())
            );
        }

        // Bước 3: Xáo trộn và bốc ngẫu nhiên theo cụm câu hỏi (giữ nguyên vẹn chùm Part 3, 4, 6, 7 không bị xé lẻ)
        List<Question> picked = pickClusterAwareQuestions(pool, request.getQuestionCount());

        // Bước 4: Nạp batch thông tin Ability cho các câu hỏi vừa bốc để trả về DTO đầy đủ
        List<Long> pickedIds = picked.stream().map(Question::getId).collect(Collectors.toList());
        AbilityLookup abilityLookup = loadAbilityLookup(pickedIds);

        List<ModuleTestQuestionItemResponse> responses = new ArrayList<>(picked.size());
        for (int i = 0; i < picked.size(); i++) {
            Question q = picked.get(i);
            responses.add(moduleTestMapper.toTransientQuestionItem(
                    q,
                    i + 1,
                    DEFAULT_QUESTION_POINTS,
                    extractQuestionSummary(q.getQuestionData()),
                    abilityLookup.idsByQuestionId().getOrDefault(q.getId(), Collections.emptyList()),
                    abilityLookup.namesByQuestionId().getOrDefault(q.getId(), Collections.emptyList()),
                    moduleAbilityIds
            ));
        }

        return responses;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Module findModuleByIdOrThrow(Long moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy mô-đun với ID: " + moduleId
                ));
    }

    private Set<Long> extractModuleAbilityIds(Module module) {
        if (module.getModuleAbilities() == null || module.getModuleAbilities().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (ModuleAbility ma : module.getModuleAbilities()) {
            if (ma.getAbility() != null && ma.getAbility().getId() != null) {
                ids.add(ma.getAbility().getId());
            }
        }
        return ids;
    }

    private long countAvailableAbilityQuestions(Module module, Set<Long> moduleAbilityIds) {
        if (!moduleAbilityIds.isEmpty()) {
            return questionRepository.countPlayableQuestionsByAbilityIds(moduleAbilityIds);
        }
        if (module.getPart() != null && module.getPart().getId() != null) {
            return questionRepository.countPlayableQuestionsByPartId(module.getPart().getId());
        }
        return 0L;
    }

    private List<Question> loadQuestionPoolForModule(
            Module module,
            Set<Long> moduleAbilityIds,
            boolean onlyMatchingAbilities
    ) {
        if (onlyMatchingAbilities && !moduleAbilityIds.isEmpty()) {
            return questionRepository.findPlayableQuestionsByAbilityIds(moduleAbilityIds);
        }
        if (module.getPart() != null && module.getPart().getId() != null) {
            return questionRepository.findPlayableQuestionsByPartId(module.getPart().getId());
        }
        return Collections.emptyList();
    }

    /**
     * Đồng bộ danh sách câu hỏi thủ công cho bài thi vượt ải:
     * 1. Xác thực tồn tại và tự động bóc tách bản ghi cha của chùm (Part 3, 4, 6, 7) thành các câu con playable.
     * 2. Kiểm tra cảnh báo sư phạm nếu có câu hỏi lệch Ability của Module (Ngoại lệ 6b).
     * 3. Diff-update an toàn: bảo toàn ID câu không đổi và chặn gỡ câu đã có lịch sử trả lời trong module_test_answers.
     *
     * @return Tổng số câu hỏi thực tế sau khi bóc tách chùm và lưu xuống DB
     */
    private int syncManualQuestions(
            Module module,
            SaveModuleTestConfigRequest request,
            Set<Long> moduleAbilityIds
    ) {
        List<Long> rawIds = request.getQuestionIds() != null
                ? request.getQuestionIds()
                : Collections.emptyList();

        if (rawIds.isEmpty()) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Vui lòng chọn ít nhất 1 câu hỏi cho bài thi vượt ải ở chế độ chọn thủ công"
            );
        }

        // Bước 1: Kiểm tra không trùng lặp ID trong cùng request
        Set<Long> seenRaw = new HashSet<>();
        for (Long qId : rawIds) {
            if (qId == null || !seenRaw.add(qId)) {
                throw new BaseException(
                        ErrorCode.BAD_REQUEST,
                        "Danh sách chứa mã câu hỏi không hợp lệ hoặc bị trùng lặp"
                );
            }
        }

        // Bước 2: Nạp theo lô các Question và kiểm tra tồn tại
        Map<Long, Question> questionById = questionRepository.findAllById(rawIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        for (Long qId : rawIds) {
            if (!questionById.containsKey(qId)) {
                throw new BaseException(
                        ErrorCode.QUESTION_NOT_FOUND,
                        "Không tìm thấy câu hỏi với ID: " + qId
                );
            }
        }

        // Bước 3: Bóc tách bản ghi cha của chùm câu hỏi (Part 3, 4, 6, 7) thành câu con playable
        Map<Long, List<Question>> subQuestionsByParentId = questionRepository
                .findByParentIdInOrderByOrderNoAsc(rawIds)
                .stream()
                .filter(sub -> sub.getParent() != null)
                .collect(Collectors.groupingBy(sub -> sub.getParent().getId()));

        Map<Long, Question> resolvedMap = new LinkedHashMap<>();
        for (Long qId : rawIds) {
            List<Question> children = subQuestionsByParentId.get(qId);
            if (children != null && !children.isEmpty()) {
                for (Question child : children) {
                    resolvedMap.putIfAbsent(child.getId(), child);
                }
            } else {
                resolvedMap.putIfAbsent(qId, questionById.get(qId));
            }
        }

        // Chặn vượt ngưỡng tối đa sau khi bóc tách chùm
        if (resolvedMap.size() > MAX_MODULE_TEST_QUESTIONS) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Sau khi bóc tách chùm câu hỏi, tổng số câu (" + resolvedMap.size()
                            + ") vượt quá giới hạn tối đa " + MAX_MODULE_TEST_QUESTIONS + " câu"
            );
        }

        // Bước 4: Kiểm tra cảnh báo sư phạm 6b (câu hỏi lệch Ability đào tạo của Module)
        if (!moduleAbilityIds.isEmpty() && !Boolean.TRUE.equals(request.getConfirmMismatchAbility())) {
            AbilityLookup lookup = loadAbilityLookup(new ArrayList<>(resolvedMap.keySet()));
            boolean hasMismatch = resolvedMap.keySet().stream().anyMatch(qId -> {
                List<Long> qAbilityIds = lookup.idsByQuestionId().getOrDefault(qId, Collections.emptyList());
                return qAbilityIds.stream().noneMatch(moduleAbilityIds::contains);
            });
            if (hasMismatch) {
                throw new BaseException(ErrorCode.MODULE_TEST_ABILITY_MISMATCH);
            }
        }

        // Bước 5: Diff-update trên bảng module_test_questions (bảo vệ khóa ngoại module_test_answers)
        List<ModuleTestQuestion> existingList = moduleTestQuestionRepository
                .findByModuleIdWithQuestionOrderByOrderNoAsc(module.getId());
        Map<Long, ModuleTestQuestion> existingByQuestionId = existingList.stream()
                .collect(Collectors.toMap(mtq -> mtq.getQuestion().getId(), mtq -> mtq));

        List<ModuleTestQuestion> toDelete = existingList.stream()
                .filter(mtq -> !resolvedMap.containsKey(mtq.getQuestion().getId()))
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(ModuleTestQuestion::getId).collect(Collectors.toList());
            // Chặn xóa vật lý các câu hỏi đã có lịch sử làm bài của học viên để tránh lỗi FK và mất dữ liệu thi
            if (moduleTestAnswerRepository.existsByModuleTestQuestionIdIn(deleteIds)) {
                throw new BaseException(
                        ErrorCode.EXAM_IN_USE,
                        "Một số câu hỏi bạn muốn gỡ đã có lịch sử làm bài của học viên nên không thể xóa khỏi đề vượt ải"
                );
            }
            moduleTestQuestionRepository.deleteAll(toDelete);
            moduleTestQuestionRepository.flush();
        }

        List<ModuleTestQuestion> toSave = new ArrayList<>(resolvedMap.size());
        int orderNo = 1;
        for (Question question : resolvedMap.values()) {
            ModuleTestQuestion existing = existingByQuestionId.get(question.getId());
            if (existing != null) {
                existing.setOrderNo(orderNo);
                toSave.add(existing);
            } else {
                ModuleTestQuestion created = new ModuleTestQuestion();
                created.setModule(module);
                created.setQuestion(question);
                created.setOrderNo(orderNo);
                created.setPoints(DEFAULT_QUESTION_POINTS);
                toSave.add(created);
            }
            orderNo++;
        }

        moduleTestQuestionRepository.saveAll(toSave);
        return toSave.size();
    }

    /**
     * Bốc ngẫu nhiên {@code targetCount} câu hỏi từ kho sao cho ưu tiên giữ trọn vẹn các cụm câu chùm (Part 3, 4, 6, 7)
     * có cùng {@code parentId}, tránh xé lẻ đoạn văn/hội thoại.
     */
    private List<Question> pickClusterAwareQuestions(List<Question> pool, int targetCount) {
        Map<String, List<Question>> clustersMap = new LinkedHashMap<>();
        for (Question q : pool) {
            String clusterKey = q.getParent() != null && q.getParent().getId() != null
                    ? "P_" + q.getParent().getId()
                    : "S_" + q.getId();
            clustersMap.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(q);
        }

        List<List<Question>> clusters = new ArrayList<>(clustersMap.values());
        Collections.shuffle(clusters);

        List<Question> picked = new ArrayList<>(targetCount);
        List<Question> leftover = new ArrayList<>();
        for (List<Question> cluster : clusters) {
            if (picked.size() + cluster.size() <= targetCount) {
                picked.addAll(cluster);
            } else {
                leftover.addAll(cluster);
            }
            if (picked.size() == targetCount) {
                break;
            }
        }

        for (Question extra : leftover) {
            if (picked.size() >= targetCount) {
                break;
            }
            picked.add(extra);
        }
        return picked;
    }

    private List<ModuleTestQuestionItemResponse> buildQuestionItems(Long moduleId, Set<Long> moduleAbilityIds) {
        List<ModuleTestQuestion> mtqList = moduleTestQuestionRepository
                .findByModuleIdWithQuestionOrderByOrderNoAsc(moduleId);
        if (mtqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = mtqList.stream()
                .map(mtq -> mtq.getQuestion().getId())
                .distinct()
                .collect(Collectors.toList());
        List<Long> mtqIds = mtqList.stream()
                .map(ModuleTestQuestion::getId)
                .collect(Collectors.toList());

        AbilityLookup abilityLookup = loadAbilityLookup(questionIds);
        Set<Long> usedMtqIds = moduleTestAnswerRepository.findUsedModuleTestQuestionIds(mtqIds);

        return mtqList.stream()
                .map(mtq -> {
                    Long qId = mtq.getQuestion().getId();
                    List<Long> qAbilityIds = abilityLookup.idsByQuestionId().getOrDefault(qId, Collections.emptyList());
                    List<String> qAbilityNames = abilityLookup.namesByQuestionId().getOrDefault(qId, Collections.emptyList());
                    boolean isMatching = moduleAbilityIds.isEmpty()
                            || qAbilityIds.stream().anyMatch(moduleAbilityIds::contains);
                    boolean hasAttemptHistory = usedMtqIds.contains(mtq.getId());
                    return moduleTestMapper.toQuestionItem(
                            mtq,
                            extractQuestionSummary(mtq.getQuestion().getQuestionData()),
                            qAbilityIds,
                            qAbilityNames,
                            isMatching,
                            hasAttemptHistory
                    );
                })
                .collect(Collectors.toList());
    }

    private AbilityLookup loadAbilityLookup(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return new AbilityLookup(Collections.emptyMap(), Collections.emptyMap());
        }
        Map<Long, List<Long>> idsMap = new HashMap<>();
        Map<Long, List<String>> namesMap = new HashMap<>();
        List<QuestionAbility> links = questionAbilityRepository.findWithAbilityByQuestionIdIn(questionIds);
        for (QuestionAbility qa : links) {
            if (qa.getQuestion() == null || qa.getAbility() == null) {
                continue;
            }
            Long qId = qa.getQuestion().getId();
            idsMap.computeIfAbsent(qId, k -> new ArrayList<>()).add(qa.getAbility().getId());
            namesMap.computeIfAbsent(qId, k -> new ArrayList<>()).add(qa.getAbility().getName());
        }
        return new AbilityLookup(idsMap, namesMap);
    }

    private String extractQuestionSummary(String questionDataJson) {
        if (questionDataJson == null || questionDataJson.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(questionDataJson);
            String text = root.hasNonNull("text") ? root.get("text").asText("") : "";
            if (text.isBlank() && root.hasNonNull("context")) {
                text = root.get("context").asText("");
            }
            String trimmed = text.trim();
            if (trimmed.length() <= SUMMARY_MAX_LENGTH) {
                return trimmed;
            }
            return trimmed.substring(0, SUMMARY_MAX_LENGTH - 3) + "...";
        } catch (Exception ex) {
            log.warn("Không thể phân tích JSON questionData để lấy tóm tắt câu hỏi: {}", ex.getMessage());
            return "";
        }
    }

    private String buildNotEnoughBankMessage(long availableCount) {
        return "Ngân hàng câu hỏi thuộc năng lực này hiện chỉ có " + availableCount
                + " câu, không đủ đáp ứng số lượng câu của đề thi. Vui lòng bổ sung câu hỏi vào ngân hàng trước (A04)";
    }

    private record AbilityLookup(
            Map<Long, List<Long>> idsByQuestionId,
            Map<Long, List<String>> namesByQuestionId
    ) {
    }
}
