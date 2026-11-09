package com.example.be.service.question.implement;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.QuestionGroupItemRequest;
import com.example.be.dto.request.question.QuestionMediaItemRequest;
import com.example.be.dto.request.question.QuestionOptionRequest;
import com.example.be.dto.request.question.UpdateQuestionGroupRequest;
import com.example.be.dto.request.question.UpdateStandaloneQuestionRequest;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.dto.response.question.QuestionListItemResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaKind;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.question.QuestionMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.repository.question.DiagnosticTestQuestionRepository;
import com.example.be.repository.question.ModuleTestQuestionRepository;
import com.example.be.repository.question.QuestionRepository;
import com.example.be.service.media.service.MediaService;
import com.example.be.service.question.service.QuestionService;
import com.example.be.service.question.util.MediaKindResolver;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Quản lý Ngân hàng câu hỏi (UC A04).
 * Tuân thủ quy chuẩn phân tầng, kiểm tra phân quyền RBAC và validation đa lớp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final int MIN_GROUP_ITEMS = 2;
    private static final int MAX_GROUP_ITEMS = 5;

    private final QuestionRepository questionRepository;
    private final PartRepository partRepository;
    private final AbilityRepository abilityRepository;
    private final MediaRepository mediaRepository;
    private final DiagnosticTestQuestionRepository diagnosticTestQuestionRepository;
    private final ModuleTestQuestionRepository moduleTestQuestionRepository;
    private final MediaService mediaService;
    private final QuestionMapper questionMapper;

    /**
     * Lấy danh sách câu hỏi gốc trong ngân hàng với bộ lọc đa tiêu chí và phân trang.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<QuestionListItemResponse> getQuestions(
            String keyword,
            ESection section,
            Long partId,
            Long abilityId,
            Integer difficulty,
            String structureType,
            Pageable pageable
    ) {
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy các bản ghi câu hỏi gốc (câu đơn hoặc ngữ cảnh cha của chùm)
            predicates.add(cb.isNull(root.get("parent")));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (section != null) {
                predicates.add(cb.equal(root.get("sections"), section));
            }

            if (partId != null) {
                predicates.add(cb.equal(root.get("part").get("id"), partId));
            }

            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (abilityId != null) {
                Join<Object, Object> qaJoin = root.join("questionAbilities", JoinType.INNER);
                predicates.add(cb.equal(qaJoin.get("ability").get("id"), abilityId));
            }

            if (structureType != null && !structureType.isBlank()) {
                String typeUpper = structureType.trim().toUpperCase(Locale.ROOT);
                if ("STANDALONE".equals(typeUpper)) {
                    predicates.add(cb.isEmpty(root.get("subQuestions")));
                } else if ("GROUP".equals(typeUpper)) {
                    predicates.add(cb.isNotEmpty(root.get("subQuestions")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Question> questionPage = questionRepository.findAll(spec, pageable);

        if (questionPage.isEmpty()) {
            return questionPage.map(q -> questionMapper.toListItemResponse(q, 0));
        }

        // Tối ưu triệt tiêu N+1: đếm số lượng câu con của các chùm trong trang hiện tại
        List<Long> questionIds = questionPage.map(Question::getId).toList();
        List<Question> subQuestions = questionRepository.findByParentIdInOrderByOrderNoAsc(questionIds);
        Map<Long, Integer> subCountMap = new LinkedHashMap<>();
        for (Question sub : subQuestions) {
            if (sub.getParent() != null) {
                subCountMap.merge(sub.getParent().getId(), 1, Integer::sum);
            }
        }

        return questionPage.map(q -> questionMapper.toListItemResponse(
                q,
                subCountMap.getOrDefault(q.getId(), 0)
        ));
    }

    /**
     * Lấy thông tin chi tiết một câu hỏi kèm toàn bộ câu con (nếu là chùm câu hỏi).
     */
    @Override
    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionById(Long id) {
        Question question = questionRepository.findWithPartById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_NOT_FOUND, "Không tìm thấy câu hỏi với ID: " + id));

        return questionMapper.toDetailResponse(question);
    }

    /**
     * Tạo mới một câu hỏi đơn lập (Part 1, 2, 5).
     */
    @Override
    @Transactional
    public QuestionDetailResponse createStandaloneQuestion(CreateStandaloneQuestionRequest request) {
        // Bước 1: Kiểm tra tính hợp lệ của phương án trả lời và đáp án đúng (Luồng 6a)
        validateOptionsAndCorrectKey(request.getOptions(), request.getCorrectKey());

        // Bước 2: Kiểm tra Part và Ability
        Part part = findPartById(request.getPartId());
        List<Ability> abilities = validateAndGetAbilities(part, request.getAbilityIds());

        // Bước 3: Kiểm tra và lấy danh sách Media đính kèm
        List<Media> medias = validateAndGetMedias(request.getMedias(), part.getSections(), false, false);

        // Bước 4: Ánh xạ và lưu bản ghi Question
        Question question = questionMapper.toStandaloneEntity(request, part, abilities, medias);
        Question savedQuestion = questionRepository.save(question);

        // Bước 5: Kích hoạt trạng thái ACTIVE cho các tệp media đã đính kèm
        medias.forEach(mediaService::confirmMedia);

        log.info("Đã tạo câu hỏi đơn lập mới id={} bởi nhân viên quản trị", savedQuestion.getId());
        return questionMapper.toDetailResponse(savedQuestion);
    }

    /**
     * Cập nhật thông tin câu hỏi đơn lập.
     */
    @Override
    @Transactional
    public QuestionDetailResponse updateStandaloneQuestion(Long id, UpdateStandaloneQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_NOT_FOUND, "Không tìm thấy câu hỏi với ID: " + id));

        if (question.getParent() != null || (question.getSubQuestions() != null && !question.getSubQuestions().isEmpty())) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Câu hỏi này là một phần của chùm câu hỏi, vui lòng dùng tính năng chỉnh sửa chùm");
        }

        // Bước 1: Validate phương án trả lời
        validateOptionsAndCorrectKey(request.getOptions(), request.getCorrectKey());

        // Bước 2: Kiểm tra Part và Ability
        Part part = findPartById(request.getPartId());
        List<Ability> abilities = validateAndGetAbilities(part, request.getAbilityIds());

        // Bước 3: Thu thập danh sách media cũ để giải phóng nếu bị gỡ
        Set<Long> oldMediaIds = question.getQuestionMedias().stream()
                .map(qm -> qm.getMedia().getId())
                .collect(Collectors.toSet());

        List<Media> newMedias = validateAndGetMedias(request.getMedias(), part.getSections(), false, false);
        Set<Long> newMediaIds = newMedias.stream().map(Media::getId).collect(Collectors.toSet());

        // Bước 4: Cập nhật entity
        questionMapper.updateStandaloneEntity(question, request, part, abilities, newMedias);
        Question updatedQuestion = questionRepository.save(question);

        // Bước 5: Kích hoạt media mới và thu hồi media cũ bị gỡ
        newMedias.forEach(mediaService::confirmMedia);
        for (Long oldId : oldMediaIds) {
            if (!newMediaIds.contains(oldId)) {
                mediaRepository.findById(oldId).ifPresent(mediaService::releaseMedia);
            }
        }

        log.info("Đã cập nhật câu hỏi đơn lập id={}", updatedQuestion.getId());
        return questionMapper.toDetailResponse(updatedQuestion);
    }

    /**
     * Tạo mới một chùm câu hỏi chung ngữ cảnh (Part 3, 4, 6, 7) kèm danh sách câu con.
     */
    @Override
    @Transactional
    public QuestionDetailResponse createQuestionGroup(CreateQuestionGroupRequest request) {
        // Bước 1: Kiểm tra số lượng câu hỏi con (từ 2 đến 5 câu)
        if (request.getItems() == null || request.getItems().size() < MIN_GROUP_ITEMS || request.getItems().size() > MAX_GROUP_ITEMS) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Chùm câu hỏi phải có từ " + MIN_GROUP_ITEMS + " đến " + MAX_GROUP_ITEMS + " câu hỏi con");
        }

        // Bước 2: Kiểm tra Part
        Part part = findPartById(request.getPartId());

        // Bước 3: Kiểm tra media chung của chùm
        List<Media> groupMedias = validateAndGetMedias(request.getMedias(), part.getSections(), true, false);

        // Với chùm Reading Part 6, 7: Bắt buộc phải có đoạn văn hoặc ít nhất 1 ảnh bài đọc
        if (part.getSections() == ESection.READING) {
            boolean hasPassage = request.getPassage() != null && !request.getPassage().isBlank();
            boolean hasImage = groupMedias.stream().anyMatch(m -> MediaKindResolver.resolve(m) == EMediaKind.IMAGE);
            if (!hasPassage && !hasImage) {
                throw new BaseException(ErrorCode.QUESTION_INVALID, "Chùm câu hỏi Reading bắt buộc phải có đoạn văn đọc hiểu hoặc hình ảnh văn bản đính kèm");
            }
        }

        // Bước 4: Tạo Entity cha
        Question groupParent = questionMapper.toGroupParentEntity(request, part, groupMedias);

        // Bước 5: Tạo các câu con và tính toán độ khó trung bình cho chùm
        int totalDifficulty = 0;
        int order = 1;
        List<Media> allItemMedias = new ArrayList<>();

        for (QuestionGroupItemRequest itemReq : request.getItems()) {
            validateOptionsAndCorrectKey(itemReq.getOptions(), itemReq.getCorrectKey());
            List<Ability> itemAbilities = validateAndGetAbilities(part, itemReq.getAbilityIds());
            List<Media> itemMedias = validateAndGetMedias(itemReq.getMedias(), part.getSections(), false, true);
            allItemMedias.addAll(itemMedias);

            Question itemEntity = questionMapper.buildGroupItemEntity(
                    itemReq,
                    groupParent,
                    itemAbilities,
                    itemMedias,
                    order++,
                    null
            );
            groupParent.getSubQuestions().add(itemEntity);
            totalDifficulty += itemReq.getDifficulty();
        }

        int avgDifficulty = Math.round((float) totalDifficulty / request.getItems().size());
        groupParent.setDifficulty(Math.max(1, Math.min(5, avgDifficulty)));

        Question savedGroup = questionRepository.save(groupParent);

        // Bước 6: Kích hoạt toàn bộ media liên kết
        groupMedias.forEach(mediaService::confirmMedia);
        allItemMedias.forEach(mediaService::confirmMedia);

        log.info("Đã tạo chùm câu hỏi mới id={} kèm {} câu con", savedGroup.getId(), savedGroup.getSubQuestions().size());
        return questionMapper.toDetailResponse(savedGroup);
    }

    /**
     * Cập nhật chùm câu hỏi và toàn bộ các câu hỏi con.
     */
    @Override
    @Transactional
    public QuestionDetailResponse updateQuestionGroup(Long id, UpdateQuestionGroupRequest request) {
        Question groupParent = questionRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_NOT_FOUND, "Không tìm thấy chùm câu hỏi với ID: " + id));

        if (groupParent.getParent() != null) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "ID được cung cấp là một câu hỏi con, không phải chùm câu hỏi");
        }

        if (request.getItems() == null || request.getItems().size() < MIN_GROUP_ITEMS || request.getItems().size() > MAX_GROUP_ITEMS) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Chùm câu hỏi phải có từ " + MIN_GROUP_ITEMS + " đến " + MAX_GROUP_ITEMS + " câu hỏi con");
        }

        Part part = findPartById(request.getPartId());
        List<Media> newGroupMedias = validateAndGetMedias(request.getMedias(), part.getSections(), true, false);

        // Thu thập media cũ của chùm và các câu con để thu hồi nếu bị gỡ
        Set<Long> oldMediaIds = new HashSet<>();
        groupParent.getQuestionMedias().forEach(qm -> oldMediaIds.add(qm.getMedia().getId()));
        groupParent.getSubQuestions().forEach(sub -> sub.getQuestionMedias().forEach(qm -> oldMediaIds.add(qm.getMedia().getId())));

        // Bước 1: Cập nhật thông tin chùm cha
        questionMapper.updateGroupParentEntity(groupParent, request, part, newGroupMedias);

        // Bước 2: Đồng bộ danh sách câu hỏi con (update existing, add new, remove deleted)
        Map<Long, Question> existingSubMap = groupParent.getSubQuestions().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Set<Long> requestedSubIds = request.getItems().stream()
                .map(QuestionGroupItemRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Kiểm tra an toàn: Nếu câu con bị xóa đang nằm trong đề thi, chặn không cho xóa
        List<Long> deletedSubIds = existingSubMap.keySet().stream()
                .filter(subId -> !requestedSubIds.contains(subId))
                .toList();

        if (!deletedSubIds.isEmpty()) {
            boolean inDiagnostic = diagnosticTestQuestionRepository.existsByQuestionIdIn(deletedSubIds);
            boolean inModule = moduleTestQuestionRepository.existsByQuestionIdIn(deletedSubIds);
            if (inDiagnostic || inModule) {
                throw new BaseException(
                        ErrorCode.QUESTION_IN_USE,
                        "Một số câu hỏi con bị xóa đang được sử dụng trong đề thi hoạt động, không thể loại bỏ"
                );
            }
        }

        // Xóa các câu con không còn trong request
        groupParent.getSubQuestions().removeIf(sub -> !requestedSubIds.contains(sub.getId()));

        int totalDifficulty = 0;
        int order = 1;
        List<Media> allActiveMedias = new ArrayList<>(newGroupMedias);

        for (QuestionGroupItemRequest itemReq : request.getItems()) {
            validateOptionsAndCorrectKey(itemReq.getOptions(), itemReq.getCorrectKey());
            List<Ability> itemAbilities = validateAndGetAbilities(part, itemReq.getAbilityIds());
            List<Media> itemMedias = validateAndGetMedias(itemReq.getMedias(), part.getSections(), false, true);
            allActiveMedias.addAll(itemMedias);

            Question existingSub = itemReq.getId() != null ? existingSubMap.get(itemReq.getId()) : null;
            if (existingSub != null) {
                questionMapper.buildGroupItemEntity(
                        itemReq,
                        groupParent,
                        itemAbilities,
                        itemMedias,
                        order++,
                        existingSub
                );
            } else {
                Question newSub = questionMapper.buildGroupItemEntity(
                        itemReq,
                        groupParent,
                        itemAbilities,
                        itemMedias,
                        order++,
                        null
                );
                groupParent.getSubQuestions().add(newSub);
            }
            totalDifficulty += itemReq.getDifficulty();
        }

        int avgDifficulty = Math.round((float) totalDifficulty / request.getItems().size());
        groupParent.setDifficulty(Math.max(1, Math.min(5, avgDifficulty)));

        Question savedGroup = questionRepository.save(groupParent);

        // Kích hoạt media mới và thu hồi media bị gỡ
        Set<Long> activeMediaIds = allActiveMedias.stream().map(Media::getId).collect(Collectors.toSet());
        allActiveMedias.forEach(mediaService::confirmMedia);
        for (Long oldId : oldMediaIds) {
            if (!activeMediaIds.contains(oldId)) {
                mediaRepository.findById(oldId).ifPresent(mediaService::releaseMedia);
            }
        }

        log.info("Đã cập nhật chùm câu hỏi id={}", savedGroup.getId());
        return questionMapper.toDetailResponse(savedGroup);
    }

    /**
     * Xóa câu hỏi (câu đơn hoặc toàn bộ chùm câu hỏi kèm các câu con).
     */
    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_NOT_FOUND, "Không tìm thấy câu hỏi với ID: " + id));

        // Bước 1: Thu thập toàn bộ ID của câu hỏi và các câu con (nếu có)
        List<Long> questionIds = new ArrayList<>();
        questionIds.add(id);

        List<Long> subIds = questionRepository.findSubQuestionIdsByParentId(id);
        questionIds.addAll(subIds);

        // Bước 2: Kiểm tra xem có bất kỳ câu hỏi nào đang nằm trong đề thi không (Luồng ngoại lệ 2b)
        boolean existsInDiagnostic = diagnosticTestQuestionRepository.existsByQuestionIdIn(questionIds);
        boolean existsInModuleTest = moduleTestQuestionRepository.existsByQuestionIdIn(questionIds);

        if (existsInDiagnostic || existsInModuleTest) {
            log.warn("Chặn xóa câu hỏi id={} vì đang được sử dụng trong đề thi hoạt động", id);
            throw new BaseException(
                    ErrorCode.QUESTION_IN_USE,
                    "Câu hỏi này đang được sử dụng trong đề thi hoạt động. Bạn chỉ có thể chỉnh sửa nội dung hoặc tạm ẩn câu hỏi"
            );
        }

        // Bước 3: Thu hồi các tệp media liên kết sang PENDING để cronjob tự động dọn rác
        List<QuestionMedia> allMedias = new ArrayList<>(question.getQuestionMedias());
        if (question.getSubQuestions() != null) {
            for (Question sub : question.getSubQuestions()) {
                allMedias.addAll(sub.getQuestionMedias());
            }
        }
        for (QuestionMedia qm : allMedias) {
            mediaService.releaseMedia(qm.getMedia());
        }

        // Bước 4: Xóa câu hỏi khỏi cơ sở dữ liệu (cascade sẽ tự xóa QuestionMedia và QuestionAbility)
        questionRepository.delete(question);
        log.info("Đã xóa câu hỏi id={} thành công khỏi ngân hàng", id);
    }

    // -------------------------------------------------------------------------
    // Private Validation Helpers
    // -------------------------------------------------------------------------

    private void validateOptionsAndCorrectKey(List<QuestionOptionRequest> options, String correctKey) {
        if (options == null || options.size() < 3 || options.size() > 4) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Vui lòng nhập đầy đủ các phương án trả lời (3 hoặc 4 phương án)");
        }

        if (correctKey == null || correctKey.isBlank()) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Vui lòng nhập nội dung câu hỏi và phương án đúng");
        }

        String normalizedCorrectKey = correctKey.trim().toUpperCase(Locale.ROOT);
        boolean correctKeyFound = false;
        Set<String> keysSeen = new HashSet<>();

        for (QuestionOptionRequest opt : options) {
            if (opt.getKey() == null || opt.getKey().isBlank()) {
                throw new BaseException(ErrorCode.QUESTION_INVALID, "Nhãn phương án trả lời không được để trống");
            }
            String key = opt.getKey().trim().toUpperCase(Locale.ROOT);
            if (!keysSeen.add(key)) {
                throw new BaseException(ErrorCode.QUESTION_INVALID, "Các nhãn phương án trả lời không được trùng lặp: " + key);
            }
            if (key.equals(normalizedCorrectKey)) {
                correctKeyFound = true;
            }
        }

        if (!correctKeyFound) {
            throw new BaseException(
                    ErrorCode.QUESTION_INVALID,
                    "Phương án đúng (" + normalizedCorrectKey + ") phải nằm trong danh sách các lựa chọn (" + String.join(", ", keysSeen) + ")"
            );
        }
    }

    private Part findPartById(Long partId) {
        if (partId == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Phần thi Part không được để trống");
        }
        return partRepository.findById(partId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phần thi Part với ID: " + partId));
    }

    private List<Ability> validateAndGetAbilities(Part part, List<Long> abilityIds) {
        if (abilityIds == null || abilityIds.isEmpty()) {
            throw new BaseException(ErrorCode.QUESTION_INVALID, "Cần gán ít nhất một kỹ năng (Ability) cho câu hỏi");
        }

        List<Ability> abilities = abilityRepository.findAllById(abilityIds);
        if (abilities.size() != abilityIds.size()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Một hoặc nhiều kỹ năng (Ability) không tồn tại trong hệ thống");
        }

        for (Ability ability : abilities) {
            if (ability.getPart() == null || !ability.getPart().getId().equals(part.getId())) {
                throw new BaseException(
                        ErrorCode.QUESTION_INVALID,
                        "Kỹ năng '" + ability.getName() + "' không thuộc về phần thi " + part.getName()
                );
            }
        }
        return abilities;
    }

    private List<Media> validateAndGetMedias(
            List<QuestionMediaItemRequest> mediaRequests,
            ESection section,
            boolean isGroupParent,
            boolean isGroupItem
    ) {
        if (mediaRequests == null || mediaRequests.isEmpty()) {
            // Kiểm tra ràng buộc bắt buộc có audio cho Listening
            if (section == ESection.LISTENING && !isGroupItem) {
                throw new BaseException(
                        ErrorCode.QUESTION_MEDIA_INVALID,
                        "Câu hỏi thuộc phần thi LISTENING bắt buộc phải có tệp âm thanh (Audio) đính kèm"
                );
            }
            return Collections.emptyList();
        }

        List<Long> mediaIds = mediaRequests.stream()
                .map(QuestionMediaItemRequest::getMediaId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Media> medias = mediaRepository.findAllById(mediaIds);
        if (medias.size() != mediaIds.size()) {
            throw new BaseException(ErrorCode.MEDIA_NOT_FOUND, "Một hoặc nhiều tệp media không tồn tại trong hệ thống");
        }

        long audioCount = 0;
        long videoCount = 0;
        long imageCount = 0;

        for (Media media : medias) {
            EMediaKind kind = MediaKindResolver.resolve(media);
            switch (kind) {
                case AUDIO -> audioCount++;
                case VIDEO -> videoCount++;
                case IMAGE -> imageCount++;
            }
        }

        // Quy tắc Section LISTENING
        if (section == ESection.LISTENING) {
            if (isGroupItem && audioCount > 0) {
                throw new BaseException(
                        ErrorCode.QUESTION_MEDIA_INVALID,
                        "Câu hỏi con trong chùm Part 3/4 kế thừa audio chung của chùm, không được gán tệp audio riêng"
                );
            }
            if (!isGroupItem && audioCount == 0) {
                throw new BaseException(
                        ErrorCode.QUESTION_MEDIA_INVALID,
                        "Câu hỏi thuộc phần thi LISTENING bắt buộc phải có tệp âm thanh (Audio) đính kèm"
                );
            }
        }

        // Quy tắc Section READING
        if (section == ESection.READING && audioCount > 0) {
            throw new BaseException(
                    ErrorCode.QUESTION_MEDIA_INVALID,
                    "Phần thi READING không được phép đính kèm tệp âm thanh (Audio)"
            );
        }

        if (audioCount > 1) {
            throw new BaseException(ErrorCode.QUESTION_MEDIA_INVALID, "Mỗi câu hỏi hoặc chùm câu hỏi chỉ được đính kèm tối đa 1 tệp âm thanh");
        }
        if (videoCount > 1) {
            throw new BaseException(ErrorCode.QUESTION_MEDIA_INVALID, "Mỗi câu hỏi chỉ được đính kèm tối đa 1 video bài giảng");
        }
        if (imageCount > 3) {
            throw new BaseException(ErrorCode.QUESTION_MEDIA_INVALID, "Mỗi câu hỏi chỉ được đính kèm tối đa 3 hình ảnh");
        }

        return medias;
    }
}

