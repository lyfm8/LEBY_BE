package com.example.be.service.content.implement;

import com.example.be.dto.request.content.CreateLessonRequest;
import com.example.be.dto.request.content.LessonScriptDto;
import com.example.be.dto.request.content.PracticeQuestionItemDto;
import com.example.be.dto.request.content.ReorderLessonsRequest;
import com.example.be.dto.request.content.SaveLessonScriptsRequest;
import com.example.be.dto.request.content.SavePracticeQuestionsRequest;
import com.example.be.dto.request.content.UpdateLessonRequest;
import com.example.be.dto.response.content.LessonDetailResponse;
import com.example.be.dto.response.content.LessonSummaryResponse;
import com.example.be.dto.response.content.PracticeQuestionResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonScript;
import com.example.be.entity.content.Module;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.content.LessonMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.content.LessonScriptRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.content.service.AdminLessonService;
import com.example.be.service.media.service.MediaService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Triển khai nghiệp vụ Quản lý Bài học (Lesson), Kịch bản phụ đề (Script)
 * và Câu hỏi luyện tập (PracticeQuestion) dành cho Quản trị viên và Giáo viên.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLessonServiceImpl implements AdminLessonService {

    private static final int MAX_PRACTICE_QUESTIONS_PER_LESSON = 100;
    private static final int DEFAULT_PRACTICE_POINTS = 10;
    private static final int SUMMARY_MAX_LENGTH = 160;

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final AbilityRepository abilityRepository;
    private final LessonScriptRepository lessonScriptRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final QuestionRepository questionRepository;
    private final LessonMapper lessonMapper;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LessonSummaryResponse> getLessonsByModuleId(Long moduleId) {
        ensureModuleExists(moduleId);
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderNoAsc(moduleId);
        return lessons.stream().map(lessonMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonById(Long lessonId) {
        Lesson lesson = findLessonByIdOrThrow(lessonId);
        List<LessonScript> scripts = lessonScriptRepository.findByLessonIdOrderByOrderNoAsc(lesson.getId());
        List<PracticeQuestionResponse> practiceQuestions = buildPracticeQuestionResponses(lesson.getId());
        return lessonMapper.toDetailResponse(lesson, scripts, practiceQuestions);
    }

    @Override
    @Transactional
    public LessonDetailResponse createLesson(Long moduleId, CreateLessonRequest request) {
        // Bước 1: Kiểm tra Module cha tồn tại
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + moduleId));

        // Bước 2: Tự động tính toán orderNo nếu client không truyền
        int orderNo = request.getOrderNo() != null
                ? request.getOrderNo()
                : lessonRepository.findMaxOrderNoByModuleId(moduleId).orElse(0) + 1;

        // Bước 3: Tìm và kiểm tra Ability thuộc phạm vi Module/Part (nếu có gán)
        Ability ability = resolveAndValidateLessonAbility(module, request.getAbilityId());

        // Bước 4: Chuyển đổi DTO sang Entity qua LessonMapper
        Lesson lesson = lessonMapper.toEntity(request, module, ability, orderNo);

        // Bước 5: Lưu vào CSDL
        Lesson savedLesson = lessonRepository.save(lesson);

        // Bước 6: Tự động kích hoạt các file Media Cloudinary (chuyển PENDING sang ACTIVE)
        confirmMediaUrls(savedLesson.getVideoUrl(), savedLesson.getAudioUrl(), savedLesson.getImageUrl());

        return getLessonById(savedLesson.getId());
    }

    @Override
    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, UpdateLessonRequest request) {
        // Bước 1: Tìm bài học hiện có
        Lesson lesson = findLessonByIdOrThrow(lessonId);

        // Bước 2: Tìm và kiểm tra Ability thuộc phạm vi Module/Part (nếu có gán)
        Ability ability = resolveAndValidateLessonAbility(lesson.getModule(), request.getAbilityId());

        // Bước 3: Cập nhật Entity thông qua LessonMapper
        lessonMapper.updateEntity(lesson, request, ability);

        // Bước 4: Lưu thay đổi
        lessonRepository.save(lesson);

        // Bước 5: Kích hoạt các file Media Cloudinary nếu có tệp mới tải lên
        confirmMediaUrls(lesson.getVideoUrl(), lesson.getAudioUrl(), lesson.getImageUrl());

        return getLessonById(lessonId);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = findLessonByIdOrThrow(lessonId);
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(Long moduleId, ReorderLessonsRequest request) {
        ensureModuleExists(moduleId);

        List<Long> orderedIds = request.getOrderedLessonIds();
        // Tối ưu hóa: Batch query + Dirty Checking + JDBC Batching
        List<Lesson> lessons = lessonRepository.findAllById(orderedIds);
        Map<Long, Lesson> lessonMap = lessons.stream()
                .filter(l -> l.getModule() != null && l.getModule().getId().equals(moduleId))
                .collect(Collectors.toMap(Lesson::getId, l -> l));

        for (int i = 0; i < orderedIds.size(); i++) {
            Lesson lesson = lessonMap.get(orderedIds.get(i));
            if (lesson != null) {
                lesson.setOrderNo(i + 1);
            }
        }
        // Dirty Checking tự động cập nhật khi Transaction commit
    }

    @Override
    @Transactional
    public LessonDetailResponse saveLessonScripts(Long lessonId, SaveLessonScriptsRequest request) {
        // Bước 1: Kiểm tra bài học tồn tại
        Lesson lesson = findLessonByIdOrThrow(lessonId);

        // Bước 2: Đồng bộ trực tiếp qua collection của Lesson để quản lý First-Level Cache
        // orphanRemoval = true sẽ tự động delete các script cũ và insert các script mới
        if (lesson.getScripts() == null) {
            lesson.setScripts(new ArrayList<>());
        } else {
            lesson.getScripts().clear();
        }

        // Bước 3: Tạo mới và thêm vào collection
        if (request.getScripts() != null && !request.getScripts().isEmpty()) {
            for (int i = 0; i < request.getScripts().size(); i++) {
                LessonScriptDto dto = request.getScripts().get(i);
                LessonScript script = lessonMapper.toScriptEntity(dto, lesson, i + 1);
                lesson.getScripts().add(script);
            }
        }

        lessonRepository.save(lesson);
        return getLessonById(lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionResponse> getPracticeQuestions(Long lessonId) {
        // Kiểm tra bài học tồn tại trước khi truy vấn danh sách câu hỏi luyện tập
        findLessonByIdOrThrow(lessonId);
        return buildPracticeQuestionResponses(lessonId);
    }

    @Override
    @Transactional
    public LessonDetailResponse savePracticeQuestions(Long lessonId, SavePracticeQuestionsRequest request) {
        // Bước 1: Kiểm tra bài học tồn tại trong CSDL
        Lesson lesson = findLessonByIdOrThrow(lessonId);

        List<PracticeQuestionItemDto> requestedItems = request.getItems() != null
                ? request.getItems()
                : Collections.emptyList();

        // Bước 2: Nếu danh sách rỗng thì xóa toàn bộ liên kết cũ và trả về chi tiết bài học
        List<PracticeQuestion> existingList = practiceQuestionRepository.findByLessonIdOrderByOrderNoAsc(lessonId);
        if (requestedItems.isEmpty()) {
            if (!existingList.isEmpty()) {
                practiceQuestionRepository.deleteAll(existingList);
                practiceQuestionRepository.flush();
            }
            log.info("Đã gỡ toàn bộ câu hỏi luyện tập khỏi bài học lessonId={}", lessonId);
            return getLessonById(lessonId);
        }

        // Bước 3: Kiểm tra không có questionId bị gửi trùng trong cùng request
        Set<Long> rawIdSet = new HashSet<>();
        List<Long> rawQuestionIds = new ArrayList<>();
        for (PracticeQuestionItemDto item : requestedItems) {
            if (!rawIdSet.add(item.getQuestionId())) {
                throw new BaseException(
                        ErrorCode.BAD_REQUEST,
                        "Danh sách chứa câu hỏi bị trùng lặp (questionId=" + item.getQuestionId() + ")"
                );
            }
            rawQuestionIds.add(item.getQuestionId());
        }

        // Bước 4: Nạp các Question tương ứng theo lô và kiểm tra tồn tại
        Map<Long, Question> questionById = questionRepository.findAllById(rawQuestionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        for (Long qId : rawQuestionIds) {
            if (!questionById.containsKey(qId)) {
                throw new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy câu hỏi với ID: " + qId
                );
            }
        }

        // Bước 5: Nạp câu hỏi con theo lô cho trường hợp client chọn ID bản ghi cha của chùm (Part 3, 4, 6, 7)
        // Cần bóc tách thành các câu hỏi con có thể làm bài để đảm bảo 100% bản ghi practice_questions hợp lệ cho UC12
        Map<Long, List<Question>> subQuestionsByParentId = questionRepository
                .findByParentIdInOrderByOrderNoAsc(rawQuestionIds)
                .stream()
                .filter(sub -> sub.getParent() != null)
                .collect(Collectors.groupingBy(sub -> sub.getParent().getId()));

        // Bước 6: Mở rộng danh sách cuối cùng theo thứ tự giữ nguyên (khử trùng lặp nếu có)
        // LinkedHashMap giữ nguyên thứ tự hiển thị câu hỏi sau khi bóc tách chùm
        Map<Long, ResolvedPracticeItem> resolvedMap = new LinkedHashMap<>();
        for (PracticeQuestionItemDto item : requestedItems) {
            int points = item.getPoints() != null ? item.getPoints() : DEFAULT_PRACTICE_POINTS;
            List<Question> children = subQuestionsByParentId.get(item.getQuestionId());
            if (children != null && !children.isEmpty()) {
                // Nếu là bản ghi cha của chùm câu hỏi: tự động gắn từng câu con theo thứ tự orderNo
                for (Question child : children) {
                    resolvedMap.putIfAbsent(child.getId(), new ResolvedPracticeItem(child, points));
                }
            } else {
                Question question = questionById.get(item.getQuestionId());
                resolvedMap.putIfAbsent(question.getId(), new ResolvedPracticeItem(question, points));
            }
        }

        // Bảo vệ ngưỡng tối đa sau khi bóc tách chùm câu hỏi (chống vượt giới hạn DoS)
        if (resolvedMap.size() > MAX_PRACTICE_QUESTIONS_PER_LESSON) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Sau khi bóc tách chùm câu hỏi, tổng số câu hỏi (" + resolvedMap.size()
                            + ") vượt quá giới hạn tối đa " + MAX_PRACTICE_QUESTIONS_PER_LESSON + " câu/bài"
            );
        }

        // Bước 7: Đồng bộ an toàn theo cơ chế In-Place Diff để tránh lỗi Duplicate Key (uk_practice_lesson_question)
        Map<Long, PracticeQuestion> existingByQuestionId = existingList.stream()
                .collect(Collectors.toMap(pq -> pq.getQuestion().getId(), pq -> pq));

        // 7a. Xóa trước các bản ghi bị gỡ khỏi bài học và flush ngay xuống DB
        List<PracticeQuestion> toDelete = existingList.stream()
                .filter(pq -> !resolvedMap.containsKey(pq.getQuestion().getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            practiceQuestionRepository.deleteAll(toDelete);
            practiceQuestionRepository.flush();
        }

        // 7b. Cập nhật bản ghi giữ nguyên (bảo toàn id, createdAt) và tạo mới bản ghi vừa thêm
        List<PracticeQuestion> toSave = new ArrayList<>();
        int normalizedOrder = 1;
        for (ResolvedPracticeItem resolved : resolvedMap.values()) {
            PracticeQuestion existing = existingByQuestionId.get(resolved.question().getId());
            if (existing != null) {
                existing.setOrderNo(normalizedOrder);
                existing.setPoints(resolved.points());
                toSave.add(existing);
            } else {
                PracticeQuestion created = new PracticeQuestion();
                created.setLesson(lesson);
                created.setQuestion(resolved.question());
                created.setOrderNo(normalizedOrder);
                created.setPoints(resolved.points());
                toSave.add(created);
            }
            normalizedOrder++;
        }

        practiceQuestionRepository.saveAll(toSave);
        log.info("Đã lưu {} câu hỏi luyện tập cho bài học lessonId={}", toSave.size(), lessonId);
        return getLessonById(lessonId);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Nạp danh sách PracticeQuestionResponse của bài học không bị N+1 Query:
     * Query 1 lấy PracticeQuestion + Question + Part + Parent;
     * Query 2 nạp theo lô QuestionAbility để lấy danh sách tên năng lực.
     *
     * @param lessonId ID của bài học
     * @return Danh sách PracticeQuestionResponse theo thứ tự orderNo tăng dần
     */
    private List<PracticeQuestionResponse> buildPracticeQuestionResponses(Long lessonId) {
        List<PracticeQuestion> practiceQuestions = practiceQuestionRepository.findAllWithQuestionAndPartByLessonId(lessonId);
        if (practiceQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        // Gom danh sách questionId để nạp batch danh sách Ability (tránh N+1 và tránh MultipleBagFetchException)
        List<Long> questionIds = practiceQuestions.stream()
                .map(pq -> pq.getQuestion().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, List<String>> abilityNamesByQuestionId = new HashMap<>();
        List<Question> questionsWithAbilities = questionRepository.findAllWithAbilitiesByIdIn(questionIds);
        for (Question q : questionsWithAbilities) {
            List<String> names = q.getQuestionAbilities() != null
                    ? q.getQuestionAbilities().stream()
                            .filter(qa -> qa.getAbility() != null)
                            .map(qa -> qa.getAbility().getName())
                            .distinct()
                            .collect(Collectors.toList())
                    : Collections.emptyList();
            abilityNamesByQuestionId.put(q.getId(), names);
        }

        return practiceQuestions.stream()
                .map(pq -> {
                    Question q = pq.getQuestion();
                    List<String> abilities = abilityNamesByQuestionId.getOrDefault(q.getId(), Collections.emptyList());
                    String summary = extractQuestionSummary(q.getQuestionData());
                    return lessonMapper.toPracticeQuestionResponse(pq, abilities, summary);
                })
                .collect(Collectors.toList());
    }

    /**
     * Trích xuất đoạn tóm tắt ngắn từ trường JSON questionData (ưu tiên thuộc tính text hoặc context).
     *
     * @param questionDataJson Chuỗi JSON của Question.questionData
     * @return Chuỗi tóm tắt đã cắt gọn tối đa 160 ký tự, hoặc chuỗi rỗng nếu không có
     */
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

    /**
     * Record nội bộ lưu cặp Question đã bóc tách cùng điểm số tương ứng khi đồng bộ PracticeQuestion.
     */
    private record ResolvedPracticeItem(Question question, int points) {
    }

    private Lesson findLessonByIdOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài học với ID: " + id));
    }

    private void ensureModuleExists(Long moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy Module với ID: " + moduleId);
        }
    }

    /**
     * Tìm và kiểm tra ràng buộc phân cấp Ability cho bài học:
     * - Nếu Module đã cấu hình danh sách Ability phụ trách (moduleAbilities), Ability của Lesson phải nằm trong danh sách đó.
     * - Nếu Module chưa cấu hình Ability riêng nhưng thuộc một Part, Ability của Lesson phải thuộc đúng Part đó.
     *
     * @param module    Module cha chứa bài học
     * @param abilityId ID của Ability cần gắn (có thể null)
     * @return Thực thể Ability hợp lệ hoặc null nếu không gắn
     * @throws BaseException Nếu không tìm thấy Ability hoặc Ability không thuộc phạm vi Module/Part
     */
    private Ability resolveAndValidateLessonAbility(Module module, Long abilityId) {
        if (abilityId == null) {
            return null;
        }

        Ability ability = abilityRepository.findById(abilityId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy Ability với ID: " + abilityId
                ));

        if (module == null) {
            return ability;
        }

        // Ưu tiên 1: Nếu Module đã cấu hình các Ability cụ thể thì Lesson chỉ được chọn trong tập đó
        if (module.getModuleAbilities() != null && !module.getModuleAbilities().isEmpty()) {
            boolean belongsToModule = module.getModuleAbilities().stream()
                    .anyMatch(ma -> ma.getAbility() != null && abilityId.equals(ma.getAbility().getId()));
            if (!belongsToModule) {
                throw new BaseException(
                        ErrorCode.BAD_REQUEST,
                        "Kỹ năng '" + ability.getName() + "' không nằm trong danh sách năng lực phụ trách của mô-đun này"
                );
            }
            return ability;
        }

        // Ưu tiên 2 (fallback): Nếu Module chưa cấu hình Ability riêng thì kiểm tra theo Part của Module
        if (module.getPart() != null
                && (ability.getPart() == null || !module.getPart().getId().equals(ability.getPart().getId()))) {
            throw new BaseException(
                    ErrorCode.BAD_REQUEST,
                    "Kỹ năng '" + ability.getName() + "' không thuộc về phần thi " + module.getPart().getName()
            );
        }

        return ability;
    }

    /**
     * Tự động kiểm tra và chuyển trạng thái các Media Cloudinary từ PENDING sang ACTIVE
     * để tránh việc bị PendingMediaCleanupScheduler tự động dọn rác xóa mất file.
     */
    private void confirmMediaUrls(String... urls) {
        if (urls == null) {
            return;
        }

        for (String url : urls) {
            if (url != null && !url.isBlank()) {
                mediaRepository.findBySecureUrl(url).ifPresent(media -> {
                    if (media.getStatus() == EMediaStatus.PENDING) {
                        mediaService.confirmMedia(media);
                        log.info("Đã kích hoạt Media ACTIVE cho bài giảng với publicId={}", media.getPublicId());
                    }
                });
            }
        }
    }
}
